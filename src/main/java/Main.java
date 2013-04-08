import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	/**
	 * @param args
	 * @throws IOException 
	 * @throws TasteException 
	 */
	public static void main(String[] args) {
		
		CliArgs cliArgs;
	    try {
	      cliArgs = CliFactory.parseArguments(CliArgs.class, args);
	    } catch (ArgumentValidationException ave) {
	      printHelp(ave.getMessage());
	      return;
	    }
	    
	    List<String> programArgsList = cliArgs.getCommands();
	    if (programArgsList == null || programArgsList.isEmpty()) {
	    	printHelp("No command specified");
	    	return;
	    }
	    String[] commandArgs = programArgsList.toArray(new String[programArgsList.size()]);

	    CliCommand command;
	    try {
	    	command = CliCommand.valueOf(commandArgs[0].toUpperCase(Locale.ENGLISH));
	    } catch (IllegalArgumentException iae) {
	    	printHelp(iae.getMessage());
	    	return;
	    }
	    
	    String file = cliArgs.getFile();
	    if (file == null){
	    	printHelp("No file specified. Plz specify file with user, item, preference, timestamp");
	    	return;
	    }
	    
	    int howMany = cliArgs.getHowMany();
	    
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	    log.info("New execution ("+command+") at " + timeStamp);
	    
	    DataModel datamodel = null;
	    RecommenderBuilder recommenderBuilder;
		try {
			recommenderBuilder = getRecommenderBuilder(cliArgs);
			
			//By default this is set to "output.txt"
			String output = cliArgs.getOutput();
			Path outputFile = Paths.get(output);
			BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
			
			datamodel = new FileDataModel(new File(file));
		  	switch (command) {
				case RECOMMEND:
					doRecommende(writer, howMany, commandArgs, recommenderBuilder, datamodel);
					break;
				case EVALUATE:
					doEvaluate(writer, commandArgs, recommenderBuilder, datamodel);
					break;
		  	}
			writer.flush();
	    }catch (IllegalArgumentException | TasteException | IOException iae) {
	    	printHelp(iae.getMessage());
	    	return;
	    }
	    
		log.info("The End");
	}
	
	private static RecommenderBuilder getRecommenderBuilder(CliArgs cliArgs) throws TasteException, IOException, IllegalArgumentException{
		
		String similarityArg = cliArgs.getSimilarity();
		int neighborhoodSize = cliArgs.getNeighborhoodSize();
		
		if (similarityArg == null)
			throw new IllegalArgumentException("Please Select similarity method {Euclidian, Pearson, Tanimoto, Spreaman, Cosine}");
		//Default value is set to 5
		if (neighborhoodSize<0)
			throw new IllegalArgumentException("Neighborhood Size must be > 0");
		
		//Throws illegal argument exception
		SimilarityMethod similarityMethod = SimilarityMethod.valueOf(similarityArg.toUpperCase(Locale.ENGLISH));

		RecommenderBuilder myRecommenderBuilder = new MyRecommenderBuilder(similarityMethod, neighborhoodSize);
		return myRecommenderBuilder;
		
	}
	
	private static void doEvaluate(BufferedWriter writer, String[] commandArgs, RecommenderBuilder recBuilder, DataModel datamodel) throws TasteException, IllegalArgumentException, IOException {
		RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
		RecommenderBuilder recommenderBuilder = recBuilder;
		DataModelBuilder dataModelBuilder = null;
		
		if (commandArgs.length != 3) {
			throw new IllegalArgumentException("Need trainingPercentage & evaluationPercentage params");
		}
		double trainingPercentage = Double.parseDouble(commandArgs[1]);
		double evaluationPercentage = Double.parseDouble(commandArgs[2]);
		log.info("Calcuate evaluation");
		double evaluation = evaluator.evaluate(recommenderBuilder, dataModelBuilder, datamodel, trainingPercentage, evaluationPercentage);
		
		writer.append("Mean Average Error:"+evaluation);
	}

	private static void doRecommende(BufferedWriter writer, int howMany, String[] commandArgs, RecommenderBuilder recBuilder, DataModel datamodel) throws TasteException, IOException {
		
	    Recommender recommender = recBuilder.buildRecommender(datamodel);
	    
	    CachingRecommender cachingRecommender = new CachingRecommender(recommender);
		if (commandArgs.length != 2) {
		    //Get recommendations for all users
			LongPrimitiveIterator userIterator = cachingRecommender.getDataModel().getUserIDs();
			while (userIterator.hasNext()) {
				Long currentUserId = userIterator.nextLong();
				log.info("Getting "+ howMany +" recommendations for user: "+currentUserId);
				output(writer,currentUserId, cachingRecommender.recommend(currentUserId, howMany));
			}
		}else {
			//Get recommendations for single user
			long userId = Long.parseLong(commandArgs[1]);
			log.info("Getting "+ howMany +" recommendations for user: "+userId);
			output(writer, userId, cachingRecommender.recommend(userId, howMany));
		}
	}

	private static void output(BufferedWriter writer, Long userId, List<RecommendedItem> items) throws IOException {
		for (RecommendedItem item : items) {
		      writer.append(userId + "," + item.getItemID() + "," + item.getValue());
		      writer.newLine();
		}
	}

	private static void printHelp(String message) {
		log.error(message);
	    System.out.println();
	    System.out.println("Mahout Recommeder Command Line Interface");
	    if (message != null) {
	    	System.out.println(message);
	    	System.out.println();
	    }
	}

}
