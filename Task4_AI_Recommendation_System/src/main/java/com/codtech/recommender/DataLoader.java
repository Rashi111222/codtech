package com.codtech.recommender;


import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class DataLoader {
     private DataModel dataModel;

    public DataModel loadData() throws IOException {
        if (dataModel == null) {
            File dataFile = new File("data/skill_ratings.csv");
            dataModel = new FileDataModel(dataFile);
        }
        return dataModel;
    }
}
