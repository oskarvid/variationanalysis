package org.campagnelab.dl.varanalysis.learning;

import it.unimi.dsi.logging.ProgressLogger;
import org.apache.commons.io.FileUtils;
import org.campagnelab.dl.model.utils.ProtoPredictor;
import org.campagnelab.dl.model.utils.mappers.AbstractPredictMutations;
import org.campagnelab.dl.model.utils.mappers.FeatureMapperV4;
import org.campagnelab.dl.model.utils.mappers.QualityFeatures;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.dl.varanalysis.storage.RecordReader;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;

/**
 * Train a neural network to predict mutations.
 * <p>
 * Created by rct2002 on 6/8/16.
 *
 * @author Remi Torracinta
 */
public class PredictMutationsV4 extends AbstractPredictMutations {
    static private Logger LOG = LoggerFactory.getLogger(PredictMutationsV4.class);

    String modelPath;
    String dataDirPath;
    String resultsPath;
    String version = "VN";
    String[] dataFilenames = new String[]{"genotypes_proto_" + version + "_mutated_randomized.parquet", "genotypes_test_proto_" + version + "_mutated_randomized.parquet"};
    String[] resultsFileNames = new String[]{"training", "test"};


    public PredictMutationsV4(String modelPath, String dataDirPath, String resultsPath) {
        this.modelPath = modelPath;
        this.dataDirPath = dataDirPath;
        this.resultsPath = resultsPath;

    }


    public static void main(String[] args) throws IOException {
        String datasetPath;
        if (args.length == 0) {
            datasetPath = "sample_data/protobuf/";
        } else {
            datasetPath = args[0];
        }
        double learningRate = 0.05;
        int miniBatchSize = 100;
        String time = "1465588623667";
        String attempt = "batch=" + miniBatchSize + "-learningRate=" + learningRate + "-time=" + time;

        PredictMutationsV4 predictor = new PredictMutationsV4(attempt, datasetPath, "tests/" + time + "/");
        predictor.PrintPredictions();
    }


    public String featuresToString(BaseInformationRecords.BaseInformation pos) {
        //indels not handled
        int[] s1Counts = new int[10];
        int[] s2Counts = new int[10];
        for (int i = 0; i < 5; i++) {
            s1Counts[i] = pos.getSamples(0).getCounts(i).getGenotypeCountForwardStrand();
            s1Counts[i + 5] = pos.getSamples(0).getCounts(i).getGenotypeCountReverseStrand();
            s2Counts[i] = pos.getSamples(1).getCounts(i).getGenotypeCountForwardStrand();
            s2Counts[i + 5] = pos.getSamples(1).getCounts(i).getGenotypeCountReverseStrand();
        }
        float[] s1Scores = new float[10];
        float[] s2Scores = new float[10];
        for (int i = 0; i < 5; i++) {
            s1Scores[i] = QualityFeatures.avgQuality(ProtoPredictor.expandFreq(pos.getSamples(0).getCounts(i).getQualityScoresForwardStrandList()));
            s1Scores[i + 5] = QualityFeatures.avgQuality(ProtoPredictor.expandFreq(pos.getSamples(0).getCounts(i).getQualityScoresReverseStrandList()));
            s2Scores[i] = QualityFeatures.avgQuality(ProtoPredictor.expandFreq(pos.getSamples(1).getCounts(i).getQualityScoresForwardStrandList()));
            s2Scores[i + 5] = QualityFeatures.avgQuality(ProtoPredictor.expandFreq(pos.getSamples(1).getCounts(i).getQualityScoresReverseStrandList()));
        }

        String features = (pos.hasFrequencyOfMutation() ? pos.getFrequencyOfMutation() : "") + "\t"
                + (pos.hasMutatedBase() ? pos.getMutatedBase() : "") + "\t"
                + pos.getReferenceIndex() + "\t"
                + pos.getPosition() + "\t"
                + pos.getReferenceBase() + "\t"
                + Arrays.toString(s1Counts) + "\t"
                + Arrays.toString(s2Counts) + "\t"
                + Arrays.toString(s1Scores) + "\t"
                + Arrays.toString(s2Scores);
        return features;
    }

    public void PrintPredictions() {
        try {
            //Load parameters from disk:
            INDArray newParams;
            DataInputStream dis = new DataInputStream(new FileInputStream(modelPath + "/bestModelParams.bin"));
            newParams = Nd4j.read(dis);

            //Load network configuration from disk:
            MultiLayerConfiguration confFromJson =
                    MultiLayerConfiguration.fromJson(FileUtils.readFileToString(new File(modelPath + "/bestModelConf.json")));

            //Create a MultiLayerNetwork from the saved configuration and parameters
            MultiLayerNetwork model = new MultiLayerNetwork(confFromJson);
            model.init();
            model.setParameters(newParams);

            File dir = new File(resultsPath);
            // attempt to create the directory here
            dir.mkdir();


            for (int i = 0; i < 2; i++) {
                //initialize results printer
                PrintWriter results = new PrintWriter(resultsPath + resultsFileNames[i], "UTF-8");
                writeHeader(results);

                //may need to adjust batch size and write outputs piecewise if test sets are very large
                //BaseInformationIterator baseIter = new BaseInformationIterator(testsetPath, Integer.MAX_VALUE, new FeatureMapperV2(), new SimpleFeatureCalculator());
                FeatureMapperV4 featureMapper = new FeatureMapperV4();
                RecordReader reader = new RecordReader(dataDirPath + dataFilenames[i]);
                //DataSet ds = baseIter.next();
//set up logger
                ProgressLogger pgReadWrite = new ProgressLogger(LOG);
                pgReadWrite.itemsName = "predict";
                pgReadWrite.expectedUpdates = reader.getTotalRecords();
                pgReadWrite.displayFreeMemory = true;
                pgReadWrite.start();


                for (BaseInformationRecords.BaseInformation record : reader) {
                    writeRecordResult(model, results, featureMapper, pgReadWrite, record);
                }

                results.close();
                pgReadWrite.stop();
            }


        } catch (Exception e) {
            throw new RuntimeException((e));
        }


    }

}