package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import classifier.DatasetEntry;

public class Helper {
	
	public static List<DatasetEntry> getData(String sparqlQuery, String endpoint) {
		final String THEME_URI = "http://www.w3.org/ns/dcat#theme";
		final String DESCRIPTION_URI = "http://purl.org/dc/terms/description";
		final String DATASET_URI = "http://www.w3.org/ns/dcat#Dataset";
		List<DatasetEntry> entries = new LinkedList<DatasetEntry>();
		if (sparqlQuery == null)
			sparqlQuery = "select * where {?s a <" + DATASET_URI + ">. ?s <" + THEME_URI
					+ "> ?o. ?s <" + DESCRIPTION_URI + "> ?d .  " + "FILTER (lang(?d) = 'en')} limit 200";
		if (endpoint == null)
			endpoint = "https://www.europeandataportal.eu/sparql";
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint);
		qef = new QueryExecutionFactoryDelay(qef, 2000);
		qef = new QueryExecutionFactoryPaginated(qef, 900);
		QueryExecution queryExecution = qef.createQueryExecution(sparqlQuery);
		ResultSet results = queryExecution.execSelect();
		while (results.hasNext()) {
			QuerySolution querySolution = results.next();
			String datasetName = querySolution.get("s").asResource().getURI();
			String description = querySolution.get("d").toString();
			String theme = querySolution.get("o").toString();
			entries.add(new DatasetEntry(datasetName, theme, description));
		}
		queryExecution.close();
		try {
			qef.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return entries;
	}
	
	public static List<DatasetEntry> getTrainData(List<DatasetEntry> fullList){
		return new Random().ints((int) (0.8*fullList.size()), 0, fullList.size()).mapToObj(fullList::get).collect(Collectors.toList());
	}
	
	public static List<DatasetEntry> getTestData(List<DatasetEntry> fullList, List<DatasetEntry> trainData){
		List<DatasetEntry> testData = new ArrayList<DatasetEntry>(fullList);
		testData.removeAll(trainData);
		return testData;
	}

}
