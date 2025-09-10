package com.service;

import com.dto.GetResult1;
import com.dto.SaveResultRequest;
import com.entity.Analysis;
import com.entity.Policy;
import com.entity.User;
import com.repository.AnalysisRepository;
import com.repository.PolicyRepository;
import com.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

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
            analysis = analysisRepo.findByPolicy_IdOrderByCreatedAtAsc(policy.getId()).getFirst();
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
}
