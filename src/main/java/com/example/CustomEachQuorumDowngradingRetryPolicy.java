package com.example;

import static com.datastax.oss.driver.api.core.servererrors.WriteType.UNLOGGED_BATCH;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.retry.RetryVerdict;
import com.datastax.oss.driver.api.core.servererrors.WriteType;
import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.internal.core.retry.ConsistencyDowngradingRetryPolicy;
import com.datastax.oss.driver.internal.core.retry.ConsistencyDowngradingRetryVerdict;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Uma extensão do {@link ConsistencyDowngradingRetryPolicy} que ao invés de diminuir a consistência 
 * de EACH_QUORUM ou QUORUM para ONE, diminui para LOCAL_QUORUM.
 * 
 * Só vai realizar a retry se a consistencia for originalmente EACH_QUORUM ou QUORUM
 * 
 */
public class CustomEachQuorumDowngradingRetryPolicy extends ConsistencyDowngradingRetryPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(CustomEachQuorumDowngradingRetryPolicy.class);
  private final String logPrefix;

  public CustomEachQuorumDowngradingRetryPolicy(DriverContext context, String profileName) {
    this(context.getSessionName() + "|" + profileName);
  }

  public CustomEachQuorumDowngradingRetryPolicy(@NonNull String logPrefix) {
    super(logPrefix);
    this.logPrefix = logPrefix;
  }

  @Override
  public RetryVerdict onReadTimeoutVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      int blockFor,
      int received,
      boolean dataPresent,
      int retryCount) {
    RetryVerdict verdict;
    if (received < blockFor) {
      verdict = maybeDowngrade(cl);
    } else {
      return super.onReadTimeoutVerdict(request, cl, blockFor, received, dataPresent, retryCount);
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace(
          VERDICT_ON_READ_TIMEOUT,
          logPrefix,
          cl,
          blockFor,
          received,
          dataPresent,
          retryCount,
          verdict);
    }
    return verdict;
  }

  @Override
  public RetryVerdict onWriteTimeoutVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      @NonNull WriteType writeType,
      int blockFor,
      int received,
      int retryCount) {
    RetryVerdict verdict;
    if (UNLOGGED_BATCH.equals(writeType)) {
      verdict = maybeDowngrade(cl);
    } else {
      return super.onWriteTimeoutVerdict(request, cl, writeType, blockFor, received, retryCount);
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace(
          VERDICT_ON_WRITE_TIMEOUT,
          logPrefix,
          cl,
          writeType,
          blockFor,
          received,
          retryCount,
          verdict);
    }
    return verdict;
  }

  @Override
  public RetryVerdict onUnavailableVerdict(
      @NonNull Request request,
      @NonNull ConsistencyLevel cl,
      int required,
      int alive,
      int retryCount) {
    RetryVerdict verdict;
    if (retryCount != 0 || cl.isSerial()) {
      return onUnavailableVerdict(request, cl, required, alive, retryCount);
    }
    verdict = maybeDowngrade(cl);
    if (LOG.isTraceEnabled()) {
      LOG.trace(VERDICT_ON_UNAVAILABLE, logPrefix, cl, required, alive, retryCount, verdict);
    }
    return verdict;    
  }

  private RetryVerdict maybeDowngrade(ConsistencyLevel current) {
    // JAVA-1005: EACH_QUORUM does not report a global number of alive replicas
    // so even if we get 0 alive replicas, there might be a node up in some other
    // datacenter
    if (current.getProtocolCode() == ConsistencyLevel.EACH_QUORUM.getProtocolCode() ||
        current.getProtocolCode() == ConsistencyLevel.QUORUM.getProtocolCode()) {
      return new ConsistencyDowngradingRetryVerdict(ConsistencyLevel.LOCAL_QUORUM);
    }
    return RetryVerdict.RETHROW;
  }

}
