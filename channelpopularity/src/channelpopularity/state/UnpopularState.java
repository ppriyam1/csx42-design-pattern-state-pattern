/**
 *
 */
package channelpopularity.state.data;

import java.util.ArrayList;
import java.util.List;

import channelpopularity.context.ContextI;
import channelpopularity.entity.Advertisement;
import channelpopularity.entity.Video;
import channelpopularity.operation.Operation;
import channelpopularity.state.AbstractState;
import channelpopularity.state.StateName;
import channelpopularity.util.Results;

public class UnpopularState extends AbstractState {

	private static final Integer RANGE = 10;

	private ContextI context;

	/**
	 * @param context
	 */
	public UnpopularState(ContextI context) {
		this.context = context;
	}

	@Override
	protected void update() {
		// updating state if the popularity score has exceeded the range of this state
		if (this.channelScore > StateName.UNPOPULAR.value)
			this.context.updateState(StateName.MILDLY_POPULAR);

		else if (this.channelScore > StateName.MILDLY_POPULAR.value
				&& this.channelScore <= StateName.HIGHLY_POPULAR.value)
			this.context.updateState(StateName.HIGHLY_POPULAR);

		else if (this.channelScore > StateName.HIGHLY_POPULAR.value
				&& this.channelScore <= StateName.ULTRA_POPULAR.value)
			this.context.updateState(StateName.ULTRA_POPULAR);
	}

	@Override
	protected void refresh() {

		final List<Integer> videoScores = new ArrayList<>();

		// re-calculating scores of each video
		this.context.getDataSource().getStore().forEach((videoName, video) -> {
			videoScores.add(this.calculatePopularity(video.getViews(), video.getLikes(), video.getDislikes()));
		});

		Integer totalVideoScore = videoScores.stream().mapToInt(Integer::intValue).sum();

		if (totalVideoScore > videoScores.size()) {

			// Calculating total score
			// Calculating average of the popularity score of all the videos
			Integer averageTotalVideoScore = totalVideoScore / videoScores.size();


			this.channelScore = averageTotalVideoScore;

		} else {
			// No update to channel score
		}

	}

	@Override
	public void add(String input) {

		String videoName = this.formatter(input);

		if (!this.context.getDataSource().getStore().containsKey(videoName)) {
			// Adding video to cache
			this.context.getDataSource().getStore().put(videoName, new Video(videoName));

			// No need to refresh score

			// Adding to result
			Results.add(StateName.UNPOPULAR, this.CONSTANT_VIDEO_ADDED, videoName);
		} else {
			System.out.println("exception: video name aleady present!");
		}
	}

	@Override
	public void remove(String input) {

		String videoName = this.formatter(input);

		if (this.context.getDataSource().getStore().containsKey(videoName)) {
			// Removing video to cache
			this.context.getDataSource().getStore().remove(videoName);

			// Need to refresh score
			refresh();

			// Updating state
			update();

			// Adding to result
			Results.add(StateName.UNPOPULAR, this.CONSTANT_VIDEO_REMOVED, videoName);
		} else {
			// TODO: Do something if Video is not present
			System.out.println("exception: video not found!");
		}
	}

	@Override
	public void request(String input) {
		final Advertisement advertisementRef = this.requestformatter(input);

		if (advertisementRef.getLength() <= RANGE) {
			// Adding to result
			Results.add(StateName.UNPOPULAR, Operation.AD_REQUEST.value, this.CONSTANT_AD_REQUEST_APPROVED);
		} else {
			// Adding to result
			Results.add(StateName.UNPOPULAR, Operation.AD_REQUEST.value, this.CONSTANT_AD_REQUEST_REJECTED);
		}

	}
}
