package de.fosd.jdime.merge;
import java.util.List;
import java.util.logging.Logger;
import de.fosd.jdime.artifact.Artifact;
import de.fosd.jdime.artifact.ast.ASTNodeArtifact;
import de.fosd.jdime.config.merge.MergeContext;
import de.fosd.jdime.config.merge.MergeScenario;
import de.fosd.jdime.config.merge.Revision;
import de.fosd.jdime.matcher.Matcher;
import de.fosd.jdime.matcher.matching.Color;
import de.fosd.jdime.matcher.matching.Matching;
import de.fosd.jdime.operations.AddOperation;
import de.fosd.jdime.operations.ConflictOperation;
import de.fosd.jdime.operations.MergeOperation;
import static de.fosd.jdime.artifact.Artifacts.root;
import static de.fosd.jdime.strdump.DumpMode.PLAINTEXT_TREE;

/**
 * @author Olaf Lessenich
 *
 * @param <T>
 *            type of artifact
 */
public class Merge<T extends Artifact<T>> implements MergeInterface<T> {
  private static final Logger LOG = Logger.getLogger(Merge.class.getCanonicalName());

  private UnorderedMerge<T> unorderedMerge = null;

  private OrderedMerge<T> orderedMerge = null;

  private String logprefix;

  /**
     * TODO: this needs high-level explanation.
     *
     * @param operation the <code>MergeOperation</code> to perform
     * @param context the <code>MergeContext</code>
     */
  @Override public void merge(MergeOperation<T> operation, MergeContext context) {
    logprefix = operation.getId() + " - ";
    MergeScenario<T> triple = operation.getMergeScenario();
    T left = triple.getLeft();
    T base = triple.getBase();
    T right = triple.getRight();
    T target = operation.getTarget();
    Revision l = left.getRevision();
    Revision b = base.getRevision();
    Revision r = right.getRevision();
    Matcher<T> matcher = null;
    Matching<T> m;
    if (!left.hasMatching(r) && !right.hasMatching(l)) {
      if (!base.isEmpty()) {
        matcher = new Matcher<>(base, left);
        m = matcher.match(context, Color.GREEN).get(base, left).get();
        if (m.getScore() == 0) {
          LOG.fine(() -> String.format("%s and %s have no matches.", base.getId(), left.getId()));
        }
        matcher = new Matcher<>(matcher, base, right);
        m = matcher.match(context, Color.GREEN).get(base, right).get();
        if (m.getScore() == 0) {
          LOG.fine(() -> String.format("%s and %s have no matches.", base.getId(), right.getId()));
        }
      }
      matcher = new Matcher<>(matcher, left, right);
      m = matcher.match(context, Color.BLUE).get(left, right).get();
      if (context.isDiffOnly() && left.isRoot() && left instanceof ASTNodeArtifact) {
        assert (right.isRoot());
        return;
      }
      if (m.getScore() == 0) {
        LOG.fine(() -> String.format("%s and %s have no matches.", left.getId(), right.getId()));
        return;
      }
    }
    if (context.isDiffOnly() && left.isRoot()) {
      assert (right.isRoot());
      return;
    }
    if (!((left.isChoice() || left.hasMatching(right)) && right.hasMatching(left))) {
      LOG.severe(left.getId() + " and " + right.getId() + " have no matches.");
      LOG.severe("left: " + root(left).dump(PLAINTEXT_TREE));
      LOG.severe("right: " + root(right).dump(PLAINTEXT_TREE));
      throw new RuntimeException();
    }
    if (target.isRoot() && !target.hasMatches()) {
      target.copyMatches(left);
    }
    List<T> leftChildren = left.getChildren();
    List<T> rightChildren = right.getChildren();
    LOG.finest(() -> String.format("%s Children that need to be merged:", prefix()));
    LOG.finest(() -> String.format("%s -> (%s)", prefix(left), leftChildren));
    LOG.finest(() -> String.format("%s -> (%s)", prefix(right), rightChildren));
    if (!left.hasChildren() || !right.hasChildren()) {
      if (!left.hasChildren() && !right.hasChildren()) {
        LOG.finest(() -> String.format("%s and [%s] have no children", prefix(left), right.getId()));
        return;
      } else {
        if (!left.hasChildren()) {
          LOG.finest(() -> String.format("%s has no children", prefix(left)));
          if (!base.hasChildren() || !right.hasChanges(b)) {
            for (T rightChild : right.getChildren()) {
              AddOperation<T> addOp = new AddOperation<>(rightChild, target, triple, r.getName());
              addOp.apply(context);
            }
            return;
          } else {
            LOG.finest(() -> String.format("%s was deleted by left", prefix(right)));
            LOG.finest(() -> String.format("%s has changes in subtree", prefix(right)));
            for (T rightChild : right.getChildren()) {
              ConflictOperation<T> conflictOp = new ConflictOperation<>(null, rightChild, target, l.getName(), r.getName());
              conflictOp.apply(context);
            }
            return;
          }
        } else {
          if (!right.hasChildren()) {
            LOG.finest(() -> String.format("%s has no children", prefix(right)));
            if (!base.hasChildren() || !left.hasChanges(b)) {
              for (T leftChild : left.getChildren()) {
                AddOperation<T> addOp = new AddOperation<>(leftChild, target, triple, l.getName());
                addOp.apply(context);
              }
              return;
            } else {
              LOG.finest(() -> String.format("%s was deleted by right", prefix(left)));
              LOG.finest(() -> String.format("%s has changes in subtree", prefix(left)));
              for (T leftChild : left.getChildren()) {
                ConflictOperation<T> conflictOp = new ConflictOperation<>(leftChild, null, target, l.getName(), r.getName());
                conflictOp.apply(context);
              }
              return;
            }
          } else {
            throw new RuntimeException("Something is very broken.");
          }
        }
      }
    }
    boolean isOrdered = false;
    for (int i = 0; !isOrdered && i < left.getNumChildren(); i++) {
      if (left.getChild(i).isOrdered()) {
        isOrdered = true;
      }
    }
    for (int i = 0; !isOrdered && i < right.getNumChildren(); i++) {
      if (right.getChild(i).isOrdered()) {
        isOrdered = true;
      }
    }
    if (!context.isDiffOnly()) {
      LOG.finest(() -> {
        String dump = root(target).dump(PLAINTEXT_TREE);
        return String.format("%s target.dumpTree() before merge:%n%s", logprefix, dump);
      });
    }
    if (isOrdered) {
      if (orderedMerge == null) {
        orderedMerge = new OrderedMerge<>();
      }
      orderedMerge.merge(operation, context);
    } else {
      if (unorderedMerge == null) {
        unorderedMerge = new UnorderedMerge<>();
      }
      unorderedMerge.merge(operation, context);
    }
  }

  /**
     * Returns the logging prefix.
     *
     * @return logging prefix
     */
  private String prefix() {
    return logprefix;
  }

  /**
     * Returns the logging prefix.
     *
     * @param artifact
     *            artifact that is subject of the logging
     * @return logging prefix
     */
  private String prefix(T artifact) {
    return String.format("%s[%s]", logprefix, (artifact == null) ? "null" : artifact.getId());
  }
}