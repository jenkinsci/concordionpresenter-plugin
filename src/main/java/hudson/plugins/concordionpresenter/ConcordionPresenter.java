package hudson.plugins.concordionpresenter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple Recorder plugin for archiving Concordion test report pages.
 *
 * @author Rob Johnston <rob@rjohnst.com>
 */
public class ConcordionPresenter extends Recorder implements Serializable {
    private static final long serialVersionUID = -6785762127770397981L;

    private final String location;

    private static volatile List<ConcordionProjectAction> actions = new ArrayList<ConcordionProjectAction>();
    static {
        actions.add(new ConcordionProjectAction());
    }

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

        listener.getLogger().println("[ConcordionPresenter] Archiving test report...");
        ConcordionBuildAction action;

        final long buildTime = build.getTimeInMillis();

        try {
            final FilePath testReport = build.getWorkspace().child(getLocation());

            if (build.getResult().isWorseOrEqualTo(Result.FAILURE) &&
                    buildTime + 1000 /*error margin*/ > testReport.lastModified()) {

                listener.getLogger().println("[ConcordionPresenter] Report looks stale, not archiving. Did tests run?");
                return true;
            }

            final FilePath target = getConcordionReportDirectory(build);

            listener.getLogger().println(String.format("[ConcordionPresenter] Archiving report from %s to %s", testReport, target));

            if (testReport.copyRecursiveTo(target) == 0) {
                listener.error("[ConcordionPresenter] Failed archiving report!");
                // don't fail builds just because we can't archive the report!
            } else {
                listener.getLogger().println("[ConcordionPresenter] Report successfully archived!");
                buildIndexFile(target);
            }

            action = new ConcordionBuildAction(build);

        } catch (Exception e) {
            if (e instanceof IOException) {
                Util.displayIOException((IOException) e, listener);
            } else {
                listener.fatalError("[ConcordionPresenter] " + e.getMessage());
            }
            e.printStackTrace(listener.fatalError("[ConcordionPresenter] Failure!"));
            return true;
        }

        build.getActions().add(action);
        return true;
    }

    private void buildIndexFile(FilePath dir) throws IOException, InterruptedException {

        dir = dir.absolutize();
        String dirPath = dir.act(new FilePath.FileCallable<String>() {
            public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                return f.getCanonicalPath();
            }
        });
        dirPath = dirPath.replace('\\', '/');
        if (!dirPath.endsWith("/")){
            dirPath+="/";
        }
        int dirPathLength = dirPath.length();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>\n");
        sb.append("<h1>Concordion Reports</h1>\n");
        sb.append("<ul>\n");

        FilePath[] reports = dir.list("**/*.html");
        for (FilePath report : reports) {
            report = report.absolutize();
            String reportPath = report.act(new FilePath.FileCallable<String>() {
                public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    return f.getCanonicalPath();
                }
            });
            reportPath = reportPath.replace('\\', '/');
            reportPath = reportPath.substring(dirPathLength);
            sb.append(String.format("<li><a href='%s'>%s</a></li>\n",reportPath,reportPath));
        }

        sb.append("</ul>\n");
        sb.append("</body></html>");

        FilePath index = new FilePath(dir, "index.html");
        index.write(sb.toString(), "UTF-8");
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public Collection<? extends Action> getProjectActions(
            AbstractProject<?, ?> project) {
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
