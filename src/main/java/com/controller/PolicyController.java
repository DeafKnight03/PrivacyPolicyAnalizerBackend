package com.controller;

import com.dto.GetResult1;
import com.dto.SaveResultRequest;
import com.service.PolicyService;
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
     * POST /api/policies
     */
    @PostMapping
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
    @GetMapping("/user/{userId}")
    public ResponseEntity<HashMap<Long, OffsetDateTime>> getPolicies(@PathVariable Long userId) {
        HashMap<Long, OffsetDateTime> result = policyService.getPolicies(userId);
        return ResponseEntity.ok(result);
    }
}
