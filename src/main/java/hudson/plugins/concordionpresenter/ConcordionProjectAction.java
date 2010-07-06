package hudson.plugins.concordionpresenter;

import hudson.model.Action;

public class ConcordionProjectAction implements Action {

	public String getDisplayName() {
		return "Latest Concordion Report";
	}

	public String getIconFileName() {
		return "clipboard.gif";
	}

	public String getUrlName() {
		return "lastCompletedBuild/concordion";
	}
}
