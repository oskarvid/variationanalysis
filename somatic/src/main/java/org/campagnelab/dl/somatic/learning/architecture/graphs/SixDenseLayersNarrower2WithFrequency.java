package org.campagnelab.dl.somatic.learning.architecture.graphs;

import org.campagnelab.dl.framework.architecture.graphs.ComputationGraphAssembler;
import org.campagnelab.dl.framework.domains.DomainDescriptor;
import org.campagnelab.dl.framework.models.ModelPropertiesHelper;
import org.campagnelab.dl.framework.tools.TrainingArguments;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.lossfunctions.ILossFunction;

/**
 * A computational graph with six dense layers and two outputs: probability of somatic mutation and frequency of the
 * mutation (0 when no mutation).
 * <p>
 *
 * @author Fabien Campagne
 */
public class SixDenseLayersNarrower2WithFrequency implements ComputationGraphAssembler {


    private int numHiddenNodes;
    private LearningRatePolicy learningRatePolicy;
    private TrainingArguments arguments;
    private int numInputs;

    private TrainingArguments args() {
        return arguments;
    }



    @Override
    public void setArguments(TrainingArguments arguments) {
        this.arguments = arguments;
    }

    @Override
    public ComputationGraph createComputationalGraph(DomainDescriptor domainDescriptor) {
        int numInputs = domainDescriptor.getNumInputs("input")[0];
        int numOutputsIsMutated = domainDescriptor.getNumOutputs("isMutated")[0];
        int numOutputsSomaticFrequency = domainDescriptor.getNumOutputs("somaticFrequency")[0];
        int numHiddenNodes = domainDescriptor.getNumHiddenNodes("firstDense");

        WeightInit WEIGHT_INIT = WeightInit.XAVIER;
        learningRatePolicy = LearningRatePolicy.Poly;
        float reductionRate = Math.min(1F, args().reductionRate);
        float modelCapacity = args().modelCapacity;

        int minimum = (int) (numHiddenNodes * Math.pow(reductionRate, 4));
        assert minimum > numOutputsIsMutated : "Too much reduction, not enough outputs: ";
        ComputationGraphConfiguration confBuilder = null;
        NeuralNetConfiguration.Builder graphBuilder = new NeuralNetConfiguration.Builder()
                .seed(args().seed)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(args().learningRate).regularization(args().regularizationRate != null).l2(args().regularizationRate != null ? args().regularizationRate : 0)
                .updater(Updater.ADAGRAD);

        if (args().dropoutRate != null) {
            graphBuilder.dropOut(args().dropoutRate);
            graphBuilder.setUseDropConnect(true);
            graphBuilder.setUseRegularization(true);
        }
        NeuralNetConfiguration.Builder graphConfiguration = graphBuilder.lrPolicyDecayRate(0.5)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .learningRate(args().learningRate)
                .seed(args().seed);
        if (args().regularizationRate != null) {
            graphConfiguration.regularization(args().regularizationRate != null);
        }
        if (args().dropoutRate != null) {
            graphConfiguration.dropOut(args().dropoutRate);
            graphConfiguration.setUseDropConnect(true);
        }

        final int nOut0 = (int) (numHiddenNodes * modelCapacity);
        final int nOut1 = (int) (numHiddenNodes * reductionRate * modelCapacity);
        final int nOut2 = (int) (numHiddenNodes * Math.pow(reductionRate, 2) * modelCapacity);
        final int nOut3 = (int) (numHiddenNodes * Math.pow(reductionRate, 3) * modelCapacity);
        final int nOut4 = (int) (numHiddenNodes * Math.pow(reductionRate, 4) * modelCapacity);

        ComputationGraphConfiguration conf = graphConfiguration
                .weightInit(WeightInit.XAVIER).graphBuilder().addInputs("input")
                .addLayer("dense1", new DenseLayer.Builder().nIn(numInputs).nOut(nOut0)
                        .weightInit(WEIGHT_INIT)
                        .activation("relu").learningRateDecayPolicy(learningRatePolicy)
                        .build(), "input")
                .addLayer("dense2", new DenseLayer.Builder().nIn(nOut0).nOut(nOut1)
                        .weightInit(WEIGHT_INIT)
                        .activation("relu").learningRateDecayPolicy(learningRatePolicy)
                        .build(), "dense1")
                .addLayer("dense3", new DenseLayer.Builder().nIn(nOut1).nOut(nOut2)
                        .weightInit(WEIGHT_INIT).learningRateDecayPolicy(learningRatePolicy)
                        .activation("relu")
                        .build(), "dense2")
                .addLayer("dense4", new DenseLayer.Builder().nIn(nOut2).nOut(nOut3)
                        .weightInit(WEIGHT_INIT).learningRateDecayPolicy(learningRatePolicy)
                        .activation("relu")
                        .build(), "dense3")
                .addLayer("dense5", new DenseLayer.Builder().nIn(nOut3).nOut(nOut4)
                        .weightInit(WEIGHT_INIT).learningRateDecayPolicy(learningRatePolicy)
                        .activation("relu")
                        .build(), "dense4")
                .addLayer("isMutated", new OutputLayer.Builder(domainDescriptor.getOutputLoss("isMutated"))
                        .weightInit(WEIGHT_INIT)
                        .activation("softmax").weightInit(WEIGHT_INIT).learningRateDecayPolicy(learningRatePolicy)
                        .nIn(nOut4).nOut(numOutputsIsMutated).build(), "dense5")
                .addLayer("somaticFrequency", new OutputLayer.Builder(domainDescriptor.getOutputLoss("somaticFrequency"))
                        .weightInit(WEIGHT_INIT)
                        .activation("identity").weightInit(WEIGHT_INIT).learningRateDecayPolicy(learningRatePolicy)
                        .nIn(nOut4).nOut(numOutputsSomaticFrequency).build(), "dense5")
                .setOutputs(getOutputNames())
                .pretrain(false).backprop(true).build();

        return new ComputationGraph(conf);
    }

    @Override
    public void setNumInputs(String inputName, int... dimension) {

    }

    @Override
    public void setNumOutputs(String outputName, int... dimension) {

    }

    @Override
    public void setNumHiddenNodes(String componentName, int numHiddenNodes) {

    }

    @Override
    public String[] getInputNames() {
        return new String[]{"input"};

    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"isMutated", "somaticFrequency"};
    }

    @Override
    public String[] getComponentNames() {
        return new String[]{"firstDense"};
    }

    @Override
    public void setLossFunction(String outputName, ILossFunction lossFunction) {

    }

    @Override
    public void saveProperties(ModelPropertiesHelper helper) {

    }
}
