package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ext.com.google.common.collect.Sets;

import classifier.DataPreProcessor;
import classifier.DatasetEntry;
import utils.Helper;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class Main {

	public static void main(String[] args) {
		Map<String, String> mapArgs = parseArguments(args);

		int ngrams = 1;
		if (mapArgs.get("-ngrams") != null)
			ngrams = Integer.valueOf(mapArgs.get("-ngrams"));

		// get, clean the data and compute word vectors
		List<DatasetEntry> entries = Helper.getData(mapArgs.get("-query"), mapArgs.get("-endpoint"));
		List<DatasetEntry> trainData = Helper.getTrainData(entries);
		List<DatasetEntry> testData = Helper.getTestData(entries, trainData);

		System.out.println("Computing vectors for training data...");
		DataPreProcessor trainCleaner = new DataPreProcessor(null);
		trainCleaner.clean(trainData, ngrams, true);
		trainCleaner.computeWordVectors(ngrams, trainData);

		System.out.println("Computing vectors for test data...");
		DataPreProcessor testCleaner = new DataPreProcessor(trainCleaner.getKnownWords());
		testCleaner.clean(testData, ngrams, false);
		testCleaner.computeWordVectors(ngrams, testData);

		ArrayList<Attribute> atts = getAttributes(trainCleaner, trainData);
		Instances trainInstances = getInstances(trainCleaner, trainData, atts, true);
		Classifier classifier = getClassifier(mapArgs.get("-c"));

		try {
			System.out.println("Building Classifier...");
			classifier.buildClassifier(trainInstances);
			Instances testInstances = getInstances(testCleaner, testData, atts, false);

			// cross validate on train data
			Evaluation eval = new Evaluation(trainInstances);
			eval.crossValidateModel(classifier, trainInstances, 4, new Random());
			System.out.println(eval.toSummaryString());

			// evaluate on test data
			List<String> actual = new ArrayList<String>();
			for(int i=0; i<testInstances.numInstances(); i++) {
				double ind = classifier.classifyInstance(testInstances.instance(i));
		        actual.add(testInstances.classAttribute().value((int) ind));
			}
			
			List<String> expected = testData.stream().map(n->n.getTheme()).collect(Collectors.toList());
			if(expected.size()!=actual.size())
				System.out.println("Oho, something is wrong here.");
			else {
				printAccuracy(actual, expected);
			}
			
			//Evaluation testEval = new Evaluation(testInstances);
			//testEval.evaluateModel(classifier, testInstances);
			//System.out.println(testEval.toSummaryString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printAccuracy(List<String> actual, List<String> expected) {
		int count=0;
		for(int i=0; i<actual.size(); i++) {
			if(actual.get(i).equals(expected.get(i)))
				count++;
		}
		System.out.println("Accuracy on test data: "+((float)count/(float) actual.size())*100+"%");
	}

	private static Classifier getClassifier(String string) {
		Classifier classifier;
		if (string == null) {
			classifier = new J48();
		} else if (string.equals("naive")) {
			classifier = new NaiveBayes();
		} else {
			classifier = new J48();
		}
		return classifier;
	}

	private static Map<String, String> parseArguments(String[] args) {
		Map<String, String> mapArgs = new HashMap<String, String>();
		if (args.length != 0) {
			for (int i = 0; i < args.length; i++) {
				String param = args[i];
				if ((i + 1) < args.length) {
					String value = args[i + 1];
					if (param.equalsIgnoreCase("-c")) {
						mapArgs.put("-c", value);
					}
					// number of vertices
					else if (param.equalsIgnoreCase("-ngrams")) {
						mapArgs.put("-ngrams", value);
					}
					// type of graph generator
					else if (param.equalsIgnoreCase("-endpoint")) {
						mapArgs.put("-endpoint", value);
					} else if (param.equalsIgnoreCase("-query")) {
						mapArgs.put("-query", value);
					}
				}
			}
		}
		return mapArgs;
	}

	private static ArrayList<Attribute> getAttributes(DataPreProcessor cleaner, List<DatasetEntry> entries) {
		Set<String> possibleCategories = entries.stream().map(DatasetEntry::getTheme).collect(Collectors.toSet());
		Set<String> lemmas = Sets.newHashSet(cleaner.getKnownWords());
		ArrayList<Attribute> atts = new ArrayList<Attribute>(
				lemmas.stream().map(attribute -> new Attribute(attribute)).collect(Collectors.toList()));
		atts.add(new Attribute("theme", new ArrayList<String>(possibleCategories)));
		return atts;
	}

	private static Instances getInstances(DataPreProcessor cleaner, List<DatasetEntry> entries,
			ArrayList<Attribute> atts, boolean isTrain) {
		// word vectors and expected class label if training
		Instances data = new Instances("DatasetInstances", atts, 0);
		data.setClass(data.attribute("theme"));
		data.setClassIndex(data.numAttributes() - 1);
		for (DatasetEntry entry : entries) {
			double[] vector = entry.getVector();
			DenseInstance ins = new DenseInstance(1.0, vector);
			ins.setDataset(data);
			if(isTrain) {
				ins.setClassValue(entry.getTheme());
			} else {
				ins.setClassMissing();;
			}
			data.add(ins);
		}
		return data;
	}
}
