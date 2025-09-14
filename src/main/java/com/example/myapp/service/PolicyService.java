package com.example.myapp.service;

import com.example.myapp.dto.GetResult1;
import com.example.myapp.dto.ProcessRunner;
import com.example.myapp.dto.SaveResultRequest;
import com.example.myapp.dto.StringDto;
import com.example.myapp.entity.Analysis;
import com.example.myapp.entity.Policy;
import com.example.myapp.entity.User;
import com.example.myapp.repository.AnalysisRepository;
import com.example.myapp.repository.PolicyRepository;
import com.example.myapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.*;

@Slf4j
@Service
public class PolicyService {

    private final AnalysisRepository analysisRepo;
    private final PolicyRepository policyRepo;
    private final UserRepository userRepo;


    public PolicyService(AnalysisRepository analysisRepo, PolicyRepository policyRepo, UserRepository userRepo) {
        this.analysisRepo = analysisRepo;
        this.policyRepo = policyRepo;
        this.userRepo = userRepo;
    }

    /**
     * Crea una nuova Policy e la relativa Analysis in un'unica transazione.
     *
     * @return l'Analysis salvata (contiene il riferimento alla Policy)
     * @throws EntityNotFoundException  se l'utente non esiste
     * @throws IllegalArgumentException se i dati in input sono invalidi
     */
    @Transactional
    public void addPolicyAndAnalysis(SaveResultRequest req) {
        // Validazioni minime (meglio usare anche Bean Validation a livello DTO)
        if (req == null) throw new IllegalArgumentException("Request must not be null");
        if (req.userId() == null) throw new IllegalArgumentException("userId is required");
        if (req.text() == null || req.text().isBlank()) throw new IllegalArgumentException("text is required");
        if (req.resJson() == null || req.resJson().isBlank()) throw new IllegalArgumentException("jsonRes is required");

        // Carica utente o 404
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + req.userId()));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Crea e salva Policy
        Policy policy = new Policy(user, req.text(), now);
        policy = policyRepo.save(policy);

        // Crea e salva Analysis collegata
        Analysis analysis = new Analysis(policy, req.resJson(), now);
        analysis = analysisRepo.save(analysis);

        //log.info("Created Policy id={} and Analysis id={} for user={}", policy.getId(), analysis.getId(), user.getId());

    }
    @Transactional
    public GetResult1 getPolicyAndAnalysis(Long policyId) {
        // Validazioni minime (meglio usare anche Bean Validation a livello DTO)
        if (policyId == null) throw new IllegalArgumentException("Request must not be null");

        // Carica utente o 404
        Policy policy = policyRepo.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("policy not found: " + policyId));
        Analysis analysis = null;
        try{
            analysis = analysisRepo.findByPolicy_IdOrderByCreatedAtAsc(policy.getId()).get(0);
        }catch(Exception e){
            throw new EntityNotFoundException("analysis not found: ");
        }

        if(analysis == null){throw new EntityNotFoundException("analysis not found");};
        return new GetResult1(policy.getContent(),analysis.getData());
        //log.info("Created Policy id={} and Analysis id={} for user={}", policy.getId(), analysis.getId(), user.getId());

    }

    @Transactional
    public HashMap<Long, OffsetDateTime> getPolicies(Long userId) {
        // Validazioni minime (meglio usare anche Bean Validation a livello DTO)
        if (userId == null) throw new IllegalArgumentException("Request must not be null");
        List<Policy> policies = policyRepo.findByUser_IdOrderByCreatedAtDesc(userId);

        HashMap<Long, OffsetDateTime> result = policies.stream()
                .collect(java.util.stream.Collectors.toMap(
                        Policy::getId,
                        Policy::getCreatedAt,
                        (existing, ignored) -> existing,
                        HashMap::new
                ));
        return result;

    }

    public StringDto analyzePolicy1(StringDto policyText) {
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path policiesDir = projectRoot.resolve("policies");
        Path outputsDir  = projectRoot.resolve("outputs");
        Path inputFile   = policiesDir.resolve("policy.txt");

        try {
            // 1) Ensure runtime dirs exist (under the project folder)
            Files.createDirectories(policiesDir);
            Files.createDirectories(outputsDir);

            // 2) Write the input policy
            Files.writeString(inputFile, policyText.stringa(), StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING);

            // 3) Prepare a temp tool dir and extract script + companions into it
            Path toolDir = Files.createTempDirectory("policy-tool-");
            Path exe     = extractTo(toolDir.resolve("policy-analyzer"),
                    "com/example/myapp/scripts/policy-analyzer");
            // JSON (rename here if your file has a different name)
            extractTo(toolDir.resolve("checklist.json"),
                    "com/example/myapp/scripts/checklist.json");
            // .env (ensure your build includes dotfiles; otherwise rename resource and still write as ".env")
            extractTo(toolDir.resolve(".env"),
                    "com/example/myapp/scripts/.env");

            // Make the script executable (Linux/macOS)
            try {
                Files.setPosixFilePermissions(exe, EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE));
            } catch (UnsupportedOperationException ignore) {
                exe.toFile().setExecutable(true, true);
            }

            // 4) Run with cwd = toolDir so relative reads (.env, checklist.json) work
            ProcessRunner.runScript(
                    List.of(exe.toString(), inputFile.toString(), "--ids", "all"),
                    toolDir.toFile()
            );

            // 5) Copy the produced output back into the project outputs/
            Path tmpOut = toolDir.resolve("outputs").resolve("analysis.json");
            Files.createDirectories(outputsDir);
            Files.copy(tmpOut, outputsDir.resolve("analysis.json"), REPLACE_EXISTING);

            String risposta = Files.readString(outputsDir.resolve("analysis.json"), StandardCharsets.UTF_8);
            return new StringDto(risposta);

        } catch (Exception e) {
            throw new RuntimeException("Analysis failed", e);
        }
    }

    /** Extract a classpath resource to a target file (overwrites if exists). */
    private static Path extractTo(Path target, String resourcePath) throws Exception {
        try (var in = Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath),
                "Missing resource: " + resourcePath)) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, REPLACE_EXISTING);
            target.toFile().deleteOnExit();
            return target;
        }
    }

}