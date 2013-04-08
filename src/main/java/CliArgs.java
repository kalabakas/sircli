import java.util.List;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface CliArgs {
	
	@Unparsed
	List<String> getCommands();
	
	@Option(helpRequest = true)
	boolean getHelp();

	@Option(defaultToNull = true, description = "File path with user, item, preference. No default value.")
	String getFile();
	
	@Option(defaultValue = "output.txt", description = "Output File path. Default output.txt")
	String getOutput();
	
	@Option(defaultToNull = true, description = "Select Similarity method. No default. (euclidian, pearson, tanimoto, spearman, cosine).")
	String getSimilarity();
	
	@Option(defaultValue = "5", description = "Select the neighborhood size. Default 5. Integer > 0.")
	int getNeighborhoodSize();
	
	@Option(defaultValue = "10", description = "How many recommendations or items to retrieve. Default 10. Integer > 0.")
	int getHowMany();
	
}
