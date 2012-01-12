package hudson.plugins.concordionpresenter;

import hudson.FilePath;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Action;

import java.io.IOException;

public class ConcordionBuildAction implements Action {

    private final AbstractBuild<?,?> build;

    public ConcordionBuildAction(final AbstractBuild<?,?> build) {
        this.build = build;
    }

    public String getDisplayName() {
        return "Concordion Report";
    }

    public String getIconFileName() {
        return "clipboard.gif";
    }

    public String getUrlName() {
        return "concordion";
    }

    public Object getTarget() {
        if (build != null) {
            return ConcordionPresenter.getConcordionReportDirectory(build);
        }
        return null;
    }

    public DirectoryBrowserSupport doDynamic(final StaplerRequest req, final StaplerResponse rsp) throws IOException, InterruptedException {
        if(this.build != null) {
            final FilePath concordionReportDirectory = ConcordionPresenter.getConcordionReportDirectory(this.build);
            return new DirectoryBrowserSupport(this.build,
                    concordionReportDirectory,
                    "concordion", "clipboard.gif", !concordionReportDirectory.child("index.html").exists());
        }
        return null;
    }
}
