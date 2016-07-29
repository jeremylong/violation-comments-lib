package se.bjurr.violations.comments.lib;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;

import com.google.common.base.Optional;

public class CommentsCreator {
 private static final String FINGERPRINT = "<this is a auto generated comment from violation-comments-lib F7F8ASD8123FSDF>";
 private static final Logger LOG = LoggerFactory.getLogger(CommentsCreator.class);

 public static void createComments(CommentsProvider commentsProvider, List<Violation> violations) {
  CommentsCreator commentsCreator = new CommentsCreator(commentsProvider, violations);
  commentsCreator.createComments();
 }

 private final CommentsProvider commentsProvider;
 private final List<ChangedFile> files;

 private final List<Violation> violations;

 private CommentsCreator(CommentsProvider commentsProvider, List<Violation> violations) {
  checkNotNull(violations, "violations");
  checkNotNull(commentsProvider, "commentsProvider");
  this.commentsProvider = commentsProvider;
  this.violations = violations;
  LOG.info(violations.size() + " violations.");
  this.files = commentsProvider.getFiles();
 }

 public void createComments() {
  List<Comment> oldComments = this.commentsProvider.getComments();
  LOG.info(oldComments.size() + " comments found.");
  oldComments = filterCommentsCreatedByThisLib(oldComments);
  LOG.info(oldComments.size() + " comments found from " + CommentsCreator.class.getSimpleName() + ", asking "
    + this.commentsProvider.getClass().getSimpleName() + " to remove them.");
  this.commentsProvider.removeComments(oldComments);
  createSingleFileComments();
  createCommentWithAllSingleFileComments();
 }

 private void createCommentWithAllSingleFileComments() {
  if (this.violations.isEmpty()) {
   LOG.info("Found no violations, not creating any comment.");
   return;
  }
  StringBuilder sb = new StringBuilder();
  sb.append("Found " + this.violations.size() + " violations:\n\n");
  for (Violation violation : this.violations) {
   String singleFileCommentContent = createSingleFileCommentContent(violation);
   sb.append(singleFileCommentContent + "\n");
  }
  LOG.info("Asking " + this.commentsProvider.getClass().getSimpleName()
    + " to create comment with all single file comments.");
  this.commentsProvider.createCommentWithAllSingleFileComments(sb.toString());
 }

 private String createSingleFileCommentContent(Violation violation) {
  String source = violation.getSource().isPresent() ? ("Source: " + violation.getSource().get() + "\n") : "";
  return ""
    + //
    "Reporter: " + violation.getReporter() + "\nRule: " + violation.getRule().or("?") + "\nSeverity: "
    + violation.getSeverity() + "\n" + //
    "File: " + violation.getFile() + " L" + violation.getStartLine() + "\n " + source + "\n" + //
    "\n" + //
    violation.getMessage() + "\n" + //
    "\n" + //
    FINGERPRINT + "\n";
 }

 private void createSingleFileComments() {
  LOG.info("Asking " + this.commentsProvider.getClass().getSimpleName() + " to comment:");
  for (Violation violation : this.violations) {
   String singleFileCommentContent = createSingleFileCommentContent(violation);
   Optional<ChangedFile> file = getFile(violation);
   if (file.isPresent()) {
    LOG.info(violation.getReporter() + " " + violation.getSeverity() + " " + violation.getRule().or("") + " "
      + file.get() + " " + violation.getStartLine() + " " + violation.getSource().or(""));
    this.commentsProvider.createSingleFileComment(file.get(), violation.getStartLine(), singleFileCommentContent);
   }
  }
 }

 private List<Comment> filterCommentsCreatedByThisLib(List<Comment> unfilteredComments) {
  List<Comment> filteredComments = newArrayList();
  for (Comment comment : unfilteredComments) {
   if (comment.getContent().contains(FINGERPRINT)) {
    filteredComments.add(comment);
   }
  }
  return filteredComments;
 }

 /**
  * When creating comment, the call should use the file as it is specified by
  * the comments provider. Not the one specified by the {@link Violation}. The
  * one in the {@link Violation} may not be recognized. <br>
  * <br>
  * Here we make a guess on which file in the {@link CommentsProvider} to use.
  */
 private Optional<ChangedFile> getFile(Violation violation) {
  for (ChangedFile providerFile : this.files) {
   if (violation.getFile().endsWith(providerFile.getFilename())
     || providerFile.getFilename().endsWith(violation.getFile())) {
    return Optional.of(providerFile);
   }
  }
  return absent();
 }
}
