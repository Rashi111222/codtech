package com.codtech.recommender;

import org.apache.mahout.cf.taste.model.DataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private JobRecommender jobRecommender;

    @Autowired
    private CertRecommender certRecommender;

    @Autowired
    private MahoutEngine mahoutEngine;

    @Autowired DataLoader dataLoader;
    @GetMapping("/recommend/jobs/{userId}")
    public Map<String, Object> getJobRecommendations(@PathVariable long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, String>> jobs = jobRecommender.getRecommendations(userId, 5);
            response.put("success", true);
            response.put("userId", userId);
            response.put("recommendations", jobs);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @GetMapping("/recommend/certs/{userId}")
    public Map<String, Object> getCertRecommendations(@PathVariable long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, String>> certs = certRecommender.getRecommendations(userId, 5);
            response.put("success", true);
            response.put("userId", userId);
            response.put("recommendations", certs);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    @GetMapping("/similarity/{userId1}/{userId2}")
    public Map<String, Object> getUserSimilarity(
            @PathVariable long userId1,
            @PathVariable long userId2) {
        Map<String, Object> response = new HashMap<>();
        try {
            double similarity = mahoutEngine.getUserSimilarity(userId1, userId2);
            double percentage = Math.round(Math.abs(similarity) * 100);
            response.put("success", true);
            response.put("userId1", userId1);
            response.put("userId2", userId2);
            response.put("similarity", similarity);
            response.put("similarityPercentage", percentage + "%");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, String>> users = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Map<String, String> user = new HashMap<>();
                user.put("userId", String.valueOf(i));
                user.put("name", "User " + i);
                users.add(user);
            }
            response.put("success", true);
            response.put("users", users);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @GetMapping("/debug/{userId}")
public Map<String, Object> debug(@PathVariable long userId) {
    Map<String, Object> response = new HashMap<>();
    try {
        DataModel model = dataLoader.loadData();
        response.put("numUsers", model.getNumUsers());
        response.put("numItems", model.getNumItems());
        response.put("userExists", model.getPreferencesFromUser(userId).length());
    } catch (Exception e) {
        response.put("error", e.getMessage());
    }
    return response;
}
}