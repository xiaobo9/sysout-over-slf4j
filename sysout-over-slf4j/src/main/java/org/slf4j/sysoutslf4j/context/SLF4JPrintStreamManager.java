package org.slf4j.sysoutslf4j.context;

import java.io.PrintStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.sysoutslf4j.common.ReflectionUtils;
import org.slf4j.sysoutslf4j.common.SLF4JPrintStream;
import org.slf4j.sysoutslf4j.common.SystemOutput;
import org.slf4j.sysoutslf4j.context.exceptionhandlers.ExceptionHandlingStrategy;
import org.slf4j.sysoutslf4j.context.exceptionhandlers.ExceptionHandlingStrategyFactory;
import org.slf4j.sysoutslf4j.system.SLF4JPrintStreamImpl;
import org.slf4j.sysoutslf4j.system.SLF4JPrintStreamConfigurator;

class SLF4JPrintStreamManager {

	private final Logger log = LoggerFactory.getLogger(SysOutOverSLF4J.class);

	void sendSystemOutAndErrToSLF4J(final LogLevel outLevel, final LogLevel errLevel,
			final ExceptionHandlingStrategyFactory exceptionHandlingStrategyFactory) {
		makeSystemOutputsSLF4JPrintStreamsIfNecessary();
		sendSystemOutAndErrToSLF4JForThisContext(outLevel, errLevel, exceptionHandlingStrategyFactory);
		log.info("Redirected System.out and System.err to SLF4J for this context");
	}

	private void makeSystemOutputsSLF4JPrintStreamsIfNecessary() {
		if (systemOutputsAreSLF4JPrintStreams()) {
			log.debug("System.out and System.err are already SLF4JPrintStreams");
		} else {
			makeSystemOutputsSLF4JPrintStreams();
			log.info("Replaced standard System.out and System.err PrintStreams with SLF4JPrintStreams");
		}
	}

	private boolean systemOutputsAreSLF4JPrintStreams() {
		return System.out.getClass().getName().equals(SLF4JPrintStreamImpl.class.getName());
	}

	private void makeSystemOutputsSLF4JPrintStreams() {
		ReflectionUtils.invokeStaticMethod("replaceSystemOutputsWithSLF4JPrintStreams", getSlf4jPrintStreamConfiguratorClass());
	}

	private Class<?> getSlf4jPrintStreamConfiguratorClass() {
		Class<?> slf4jPrintStreamConfiguratorClass;
		if (systemOutputsAreSLF4JPrintStreams()) {
			ClassLoader classLoader = System.out.getClass().getClassLoader();
			slf4jPrintStreamConfiguratorClass =
				ClassLoaderUtils.loadClass(classLoader, SLF4JPrintStreamConfigurator.class);
		} else {
			try {
				slf4jPrintStreamConfiguratorClass =
					ClassLoader.getSystemClassLoader().loadClass(SLF4JPrintStreamConfigurator.class.getName());
			} catch (ClassNotFoundException cnfe) {
				try {
					URL jarUrl = ClassLoaderUtils.getJarURL(SLF4JPrintStreamConfigurator.class);
					ReflectionUtils.invokeMethod("addUrl", ClassLoader.getSystemClassLoader(), URL.class, jarUrl);
					slf4jPrintStreamConfiguratorClass =
						ClassLoader.getSystemClassLoader().loadClass(SLF4JPrintStreamConfigurator.class.getName());
				} catch (Exception e) {
					slf4jPrintStreamConfiguratorClass = SLF4JPrintStreamConfigurator.class;
				}
			}
		}
		return slf4jPrintStreamConfiguratorClass;
	}

	private void sendSystemOutAndErrToSLF4JForThisContext(final LogLevel outLevel, final LogLevel errLevel, 
			final ExceptionHandlingStrategyFactory exceptionHandlingStrategyFactory) {
		registerNewLoggerAppender(exceptionHandlingStrategyFactory, SLF4JPrintStreamProxy.wrap(SystemOutput.OUT.get()), outLevel);
		registerNewLoggerAppender(exceptionHandlingStrategyFactory, SLF4JPrintStreamProxy.wrap(SystemOutput.ERR.get()), errLevel);
	}

	private void registerNewLoggerAppender(
			final ExceptionHandlingStrategyFactory exceptionHandlingStrategyFactory,
			final SLF4JPrintStream slf4jPrintStream, final LogLevel logLevel) {

		final PrintStream originalPrintStream = slf4jPrintStream.getOriginalPrintStream();
		final ExceptionHandlingStrategy exceptionHandlingStrategy = 
			exceptionHandlingStrategyFactory.makeExceptionHandlingStrategy(logLevel, originalPrintStream);
		final Object loggerAppender = 
			new LoggerAppenderImpl(logLevel, exceptionHandlingStrategy, originalPrintStream);
		slf4jPrintStream.registerLoggerAppender(loggerAppender);
	}

	void stopSendingSystemOutAndErrToSLF4J() {
		try {
			for (SystemOutput systemOutput : SystemOutput.values()) {
				SLF4JPrintStream slf4jPrintStream = SLF4JPrintStreamProxy.wrap(systemOutput.get());
				slf4jPrintStream.deregisterLoggerAppender();
			}
		} catch (IllegalArgumentException iae) {
			log.warn("Cannot stop sending System.out and System.err to SLF4J - they are not being sent there at the moment");
		}
	}

	void restoreOriginalSystemOutputsIfNecessary() {
		if (systemOutputsAreSLF4JPrintStreams()) {
			restoreOriginalSystemOutputs();
			log.info("Restored original System.out and System.err");
		} else {
			log.warn("System.out and System.err are not SLF4JPrintStreams - cannot restore");
		}
	}

	private void restoreOriginalSystemOutputs() {
		ReflectionUtils.invokeStaticMethod("restoreOriginalSystemOutputs", getSlf4jPrintStreamConfiguratorClass());
	}

}