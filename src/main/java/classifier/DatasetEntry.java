package classifier;

public class DatasetEntry {
	private String datasetName;
	private String theme;
	private String description;
	private double[] vector;

	public DatasetEntry(String datasetName, String theme, String description) {
		this.datasetName = datasetName;
		this.theme = theme;
		this.description = description;
	}

	public String getDatasetName() {
		return datasetName;
	}

	public String getTheme() {
		return theme;
	}

	public String getDescription() {
		return description;
	}

	public double[] getVector() {
		return vector;
	}

	public void setVector(double[] vector) {
		this.vector = vector;
	}

}
