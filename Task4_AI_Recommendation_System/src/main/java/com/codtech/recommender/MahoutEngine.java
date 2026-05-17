package com.codtech.recommender;

import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MahoutEngine {

    @Autowired
    private DataLoader dataLoader;

    private UserBasedRecommender userBasedRecommender;
    private ItemBasedRecommender itemBasedRecommender;

    private void buildRecommenders() throws Exception {
        DataModel model = dataLoader.loadData();

        // User Based CF - Pearson Correlation
        UserSimilarity userSimilarity = new PearsonCorrelationSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, userSimilarity, model);
        userBasedRecommender = new GenericUserBasedRecommender(model, neighborhood, userSimilarity);

        // Item Based CF - Cosine Similarity
        ItemSimilarity itemSimilarity = new UncenteredCosineSimilarity(model);
        itemBasedRecommender = new GenericItemBasedRecommender(model, itemSimilarity);
    }

    public List<RecommendedItem> getJobRecommendations(long userId, int numRecommendations) throws Exception {
        if (userBasedRecommender == null) {
            buildRecommenders();
        }
        return userBasedRecommender.recommend(userId, numRecommendations);
    }

    public List<RecommendedItem> getCertRecommendations(long userId, int numRecommendations) throws Exception {
        if (itemBasedRecommender == null) {
            buildRecommenders();
        }
        return itemBasedRecommender.recommend(userId, numRecommendations);
    }

    public double getUserSimilarity(long userId1, long userId2) throws Exception {
        if (userBasedRecommender == null) {
            buildRecommenders();
        }
        DataModel model = dataLoader.loadData();
        UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
        return similarity.userSimilarity(userId1, userId2);
    }

}