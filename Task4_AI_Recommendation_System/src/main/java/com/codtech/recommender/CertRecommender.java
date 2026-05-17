package com.codtech.recommender;

import com.opencsv.CSVReader;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CertRecommender {

    @Autowired
    private MahoutEngine mahoutEngine;

    private Map<Long, String[]> certCatalog = new HashMap<>();
    private boolean catalogLoaded = false;

    private void loadCertCatalog() throws Exception {
        if (catalogLoaded) return;

        CSVReader reader = new CSVReader(new FileReader("data/certs.csv"));
        String[] line;
        reader.readNext();

        while ((line = reader.readNext()) != null) {
            Long certId = Long.parseLong(line[0]);
            String[] certDetails = {line[1], line[2], line[3], line[4], line[5]};
            certCatalog.put(certId, certDetails);
        }
        reader.close();
        catalogLoaded = true;
    }

    public List<Map<String, String>> getRecommendations(long userId, int count) throws Exception {
        loadCertCatalog();

        List<RecommendedItem> recommended = mahoutEngine.getCertRecommendations(userId, count);
        List<Map<String, String>> result = new ArrayList<>();

        for (RecommendedItem item : recommended) {
            long certId = item.getItemID();
            float score = item.getValue();

            String[] details = certCatalog.get(certId);
            if (details != null) {
                Map<String, String> cert = new HashMap<>();
                cert.put("certId", String.valueOf(certId));
                cert.put("name", details[0]);
                cert.put("provider", details[1]);
                cert.put("relatedJob", details[2]);
                cert.put("difficulty", details[3]);
                cert.put("link", details[4]);
                cert.put("matchScore", String.format("%.0f%%", (score / 5.0) * 100));
                result.add(cert);
            }
        }
        return result;
    }
}
