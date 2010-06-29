package com.rjohnst.hudson.plugins.concordionpresenter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;

/**
 * Simple Recorder plugin for archiving Concordion test report pages.
 * 
 * @author Rob Johnston <rob@rjohnst.com>
 */
public class ConcordionPresenter extends Recorder implements Serializable {
	private static final long serialVersionUID = -6785762127770397981L;

	private final String location;

	private static volatile List<ConcordionProjectAction> actions = new ArrayList<ConcordionProjectAction>();

	@DataBoundConstructor
	public ConcordionPresenter(final String location) {
		this.location = location;
	}

	protected static FilePath getConcordionReportDirectory(final AbstractBuild<?, ?> build) {
		return new FilePath(new File(build.getRootDir(), "concordion"));
	}

	public String getLocation() {
		return location;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build,
			final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {

		listener.getLogger().println("Archiving Concordion test report...");
		ConcordionBuildAction action;

		final long buildTime = build.getTimeInMillis();

		try {
			final FilePath testReport = build.getWorkspace().child(getLocation());

			if (build.getResult().isWorseOrEqualTo(Result.FAILURE) &&
					buildTime + 1000 /*error margin*/ > testReport.lastModified()) {

				listener.getLogger().println("Concordion report looks stale, not archiving. Did tests run?");
				return true;
			}

			final FilePath target = getConcordionReportDirectory(build);

			listener.getLogger().println(String.format("Archiving report from %s to %s", testReport, target));

			testReport.copyRecursiveTo(target);
			action = new ConcordionBuildAction(build);

		} catch (IOException e) {
			e.printStackTrace(listener.error("Failed to archive concordion report"));
			build.setResult(Result.FAILURE);
			return true;
		}

		build.getActions().add(action);
		return true;
	}

	@Override
	public BuildStepDescriptor getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public Collection<? extends Action> getProjectActions(
			AbstractProject<?, ?> project) {

		// the ProjectAction is stateless, so let's not keep creating them
		if (actions.size() == 0) {
			synchronized (this) {
				if (actions.size() == 0) {
					// double checked locking works in java5
					actions.add(new ConcordionProjectAction());
				}
			}
		}

		return actions;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public FormValidation doCheckLocation(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value) throws IOException, ServletException {
			return FilePath.validateFileMask(project.getSomeWorkspace(), value);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Publish Concordion test report";
		}

	}
}
