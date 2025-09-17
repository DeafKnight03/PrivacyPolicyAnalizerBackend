package com.example.myapp.controller;

import com.example.myapp.dto.GetResult1;
import com.example.myapp.dto.SaveResultRequest;
import com.example.myapp.dto.StringDto;
import com.example.myapp.dto.UserPoliciesList;
import com.example.myapp.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * Crea una nuova Policy + Analysis
     * POST /api/policies/save
     */
    @PostMapping("/save")
    public ResponseEntity<Void> addPolicyAndAnalysis(@RequestBody SaveResultRequest req) {
        policyService.addPolicyAndAnalysis(req);
        return ResponseEntity.ok().build();
    }

    /**
     * Recupera una Policy + prima Analysis
     * GET /api/policies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GetResult1> getPolicyAndAnalysis(@PathVariable Long id) {
        GetResult1 result = policyService.getPolicyAndAnalysis(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Recupera tutte le policy di un utente (id → createdAt)
     * GET /api/policies/user/{userId}
     */
    @PostMapping("/getPolicies")
    public ResponseEntity<UserPoliciesList> getPolicies(@RequestBody StringDto req) {
        UserPoliciesList result = policyService.getPolicies(req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze1")
    public ResponseEntity<StringDto> test(@RequestBody StringDto policyText) {
        StringDto result = policyService.analyzePolicy1(policyText);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/count")
    public ResponseEntity<StringDto> count(@RequestBody StringDto id) {
        StringDto res =  policyService.numPages(id);
        return ResponseEntity.ok(res);

    }


}
