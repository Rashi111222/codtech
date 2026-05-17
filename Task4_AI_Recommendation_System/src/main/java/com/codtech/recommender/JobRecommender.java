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
public class JobRecommender {

    @Autowired
    private MahoutEngine mahoutEngine;

    private Map<Long, String[]> jobCatalog = new HashMap<>();
    private boolean catalogLoaded = false;

    private void loadJobCatalog() throws Exception {
        if (catalogLoaded) return;

        CSVReader reader = new CSVReader(new FileReader("data/jobs.csv"));
        String[] line;
        reader.readNext();

        while ((line = reader.readNext()) != null) {
            Long jobId = Long.parseLong(line[0]);
            String[] jobDetails = {line[1], line[2], line[3], line[4]};
            jobCatalog.put(jobId, jobDetails);
        }
        reader.close();
        catalogLoaded = true;
    }

    public List<Map<String, String>> getRecommendations(long userId, int count) throws Exception {
        loadJobCatalog();

        List<RecommendedItem> recommended = mahoutEngine.getJobRecommendations(userId, count);
        List<Map<String, String>> result = new ArrayList<>();

        for (RecommendedItem item : recommended) {
            long jobId = item.getItemID();
            float score = item.getValue();

            String[] details = jobCatalog.get(jobId);
            if (details != null) {
                Map<String, String> job = new HashMap<>();
                job.put("jobId", String.valueOf(jobId));
                job.put("title", details[0]);
                job.put("skills", details[1]);
                job.put("salary", details[2]);
                job.put("category", details[3]);
                job.put("matchScore", String.format("%.0f%%", (score / 5.0) * 100));
                result.add(job);
            }
        }
        return result;
    }
}