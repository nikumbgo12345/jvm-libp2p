// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The ServiceResolver queries three times consecutively for services of a given type, and then removes itself from the timer.
 */
public class ServiceResolver extends DNSTask {
    private static Logger logger = LogManager.getLogger(ServiceResolver.class.getName());

    protected int _count = 0;
    private final String _type;
    private ScheduledFuture<?> _isShutdown;

    public ServiceResolver(JmDNSImpl jmDNSImpl, String type) {
        super(jmDNSImpl);
        this._type = type;
    }

    @Override
    protected String getName() {
        return "ServiceResolver(" + (this.dns() != null ? this.dns().getName() : "") + ")";
    }

    @Override
    public String toString() {
        return super.toString() + " count: " + _count;
    }

    @Override
    public void start() {
        _isShutdown = _scheduler.scheduleAtFixedRate(
                this,
                DNSConstants.QUERY_WAIT_INTERVAL,
                120 * 1000,
                TimeUnit.MILLISECONDS
        );
    }

    public Future<Void> stop() {
        _scheduler.shutdown();
        return (Future<Void>)_isShutdown;
    }

    @Override
    public void run() {
        try {
            logger.debug("{}.run() JmDNS {}",this.getName(), this.description());
            DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
            out = this.addQuestions(out);
            if (!out.isEmpty()) {
                this.dns().send(out);
            }
        } catch (Throwable e) {
            logger.warn(this.getName() + ".run() exception ", e);
        }
    }

    private DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        newOut = this.addQuestion(newOut, DNSQuestion.newQuestion(_type, DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
        return newOut;
    }

    private String description() {
        return "querying service";
    }
}