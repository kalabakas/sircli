import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.CachingUserSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;


public class MyRecommenderBuilder implements RecommenderBuilder {

	private SimilarityMethod userSimilarity;
	private int neighborhoodSize;
	
	public MyRecommenderBuilder(SimilarityMethod similarityMethod, int nsize) {
		
		this.userSimilarity = similarityMethod;
		this.neighborhoodSize = nsize;
	}

	@Override
	public Recommender buildRecommender(DataModel dataModel) throws TasteException {
		
		UserSimilarity userSimilarity;
		switch (this.userSimilarity){
			case EUCLIDIAN:
				userSimilarity = new EuclideanDistanceSimilarity(dataModel);
				break;
			case PEARSON:
				userSimilarity = new PearsonCorrelationSimilarity(dataModel);
				break;
			case TANIMOTO:
				userSimilarity = new TanimotoCoefficientSimilarity(dataModel);
				break;
			case SPEARMAN:
				userSimilarity = new SpearmanCorrelationSimilarity(dataModel);
				break;
			case COSINE:
				userSimilarity = new UncenteredCosineSimilarity(dataModel);
				break;
			default:
				userSimilarity = null;
		}
					
		CachingUserSimilarity caching_similarity = new CachingUserSimilarity(userSimilarity, dataModel);
		UserNeighborhood neighborhood = new NearestNUserNeighborhood(this.neighborhoodSize, caching_similarity, dataModel);
		Recommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, caching_similarity);
		return recommender;
	}

}
