package classifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.github.davidmoten.guavamini.Sets;
import com.google.common.collect.Lists;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.CoreMap;

public class DataPreProcessor {
	private List<String> knownWords;
	private List<List<String>> ngrams;
	private List<String> normalizedDocuments;
	private StanfordCoreNLP pipeline;

	public DataPreProcessor(List<String> knownWords) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		pipeline = new StanfordCoreNLP(props);
		normalizedDocuments = new ArrayList<String>();
		if(knownWords==null)
			this.knownWords = new ArrayList<String>();
		else {
			this.knownWords = knownWords;
		}
		
		ngrams = new ArrayList<List<String>>();
	}

	/**
	 * normalizes the data
	 * 
	 * @param entries
	 * @param size    desired ngrams to use
	 */
	public void clean(List<DatasetEntry> entries, int size, boolean isTrain) {
		entries.forEach((entry -> {
			List<String> docLemmas = normalize(entry.getDescription());
			String sentence = StringUtils.join(docLemmas, " ");
			normalizedDocuments.add(sentence);
			if (size == 1) {
				if(isTrain)
					knownWords.addAll(docLemmas);
				ngrams.add(docLemmas);
			}
		}));
		if (size > 1) {
			normalizedDocuments.forEach((document) -> {
				List<String> words = Lists.newArrayList(document.split("\\s+"));
				List<String> nWords = getNgrams(words, size);
				ngrams.add(nWords);
				if(isTrain)
					knownWords.addAll(nWords);
				document = StringUtils.join(nWords, " ");
			});
		}
	}

	/**
	 * vectors follow a tf-ifs
	 * 
	 * @param size
	 * @param entries
	 */
	public void computeWordVectors(int size, List<DatasetEntry> entries) {
		computeBOWWordVectors(entries);

	}

	private void computeBOWWordVectors(List<DatasetEntry> entries) {
		for (int i = 0; i < entries.size(); i++) {
			double[] bagOfWords = getBagOfN(ngrams.get(i));
			entries.get(i).setVector(bagOfWords);
		}
	}

	public static List<String> getNgrams(List<String> words, int ngramsSize) {
		List<List<String>> ng = CollectionUtils.getNGrams(words, ngramsSize, ngramsSize);
		List<String> ngrams = new ArrayList<>();
		for (List<String> n : ng)
			ngrams.add(StringUtils.join(n, " "));
		return ngrams;
	}

	private double[] getBagOfN(List<String> sentence) {
		// last index for class attr.
		double[] bagOfWords = new double[knownWords.size() + 1];
		int i = 0;
		for (String knownWord : knownWords) {
			double tfidf = calculateTFIDF(knownWord, sentence);
			bagOfWords[i] = tfidf;
			i++;
		}
		return bagOfWords;
	}

	private double calculateTFIDF(String word, List<String> sentence) {
		return calculateTF(word, sentence) * calculateIDF(normalizedDocuments.size(), word);
	}

	private double calculateTF(String word, List<String> sentence) {
		return Math.log(1 + Collections.frequency(sentence, word));
	}

	private double calculateIDF(int size, String word) {
		int noDocsTerm = getNoDocsTermAppearsIn(ngrams, word);
		if(noDocsTerm==0)
			return 0;
		return Math.log(size / noDocsTerm);
	}

	/**
	 * Case insensitive, removes stop words and lemmatizes
	 * 
	 * @param text
	 * @return
	 */
	public List<String> normalize(String text) {
		Set<String> stopWords = readStopWords();
		Pattern pattern = Pattern.compile("[^a-zA-Z\\s]");
		Matcher matcher = pattern.matcher(text);
		text = matcher.replaceAll("");
		List<String> knownWords = new LinkedList<String>();
		CoreDocument document = new CoreDocument(text.toLowerCase());
		pipeline.annotate(document);
		List<CoreMap> sentences = document.annotation().get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String lemma = token.get(LemmaAnnotation.class);
				if (lemma.length() > 1 && !stopWords.contains(lemma) && !stopWords.contains(token.word()))
					knownWords.add(lemma);
			}
		}
		return knownWords;
	}

	private int getNoDocsTermAppearsIn(List<List<String>> ngrams2, String term) {
		int count = 0;
		for (List<String> document : ngrams2) {
			if (Collections.frequency(document, term) > 0)
				count++;
		}
		return count;
	}

	/**
	 * English standard stopwords
	 * 
	 * @return
	 */
	private Set<String> readStopWords() {
		Set<String> lines = new HashSet<String>();
		try {
			lines = Sets.newHashSet(Files.readAllLines(Paths.get(getFilePath("stop_words.txt", this))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;

	}

	public static String getFilePath(String fileName, Object instance) {
		return instance.getClass().getClassLoader().getResource(fileName).getFile();
	}

	public List<List<String>> getNgrams() {
		return ngrams;
	}

	public List<String> getKnownWords() {
		return knownWords;
	}

	public List<String> getNormalizedDocuments() {
		return normalizedDocuments;
	}

	public void setKnownWords(List<String> knownWords) {
		this.knownWords = knownWords;
	}

	public void setNgrams(List<List<String>> ngrams) {
		this.ngrams = ngrams;
	}

}
