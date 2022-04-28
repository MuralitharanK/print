package io.mosip.print.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.springframework.stereotype.Component;

import io.mosip.print.spi.TemplateManager;
import io.mosip.print.spi.TemplateManagerBuilder;
import lombok.Getter;

@Getter
@Component
public class TemplateManagerBuilderImpl implements TemplateManagerBuilder {

	private String resourceLoader = "classpath";
	private String templatePath = ".";
	private boolean cache = Boolean.TRUE;
	private String defaultEncoding = StandardCharsets.UTF_8.name();

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder#
	 * resourceLoader(java.lang.String)
	 */
	@Override
	public TemplateManagerBuilder resourceLoader(String resourceLoader) {
		this.resourceLoader = resourceLoader;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder#resourcePath(
	 * java.lang.String)
	 */
	@Override
	public TemplateManagerBuilder resourcePath(String templatePath) {
		this.templatePath = templatePath;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder#enableCache(
	 * boolean)
	 */
	@Override
	public TemplateManagerBuilder enableCache(boolean cache) {
		this.cache = cache;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder#encodingType(
	 * java.lang.String)
	 */
	@Override
	public TemplateManagerBuilder encodingType(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.kernel.core.templatemanager.spi.TemplateManagerBuilder#build()
	 */
	@Override
	public TemplateManager build() {
		final Properties properties = new Properties();
		properties.put(RuntimeConstants.INPUT_ENCODING, defaultEncoding);
		properties.put(RuntimeConstants.OUTPUT_ENCODING, defaultEncoding);
		properties.put(RuntimeConstants.ENCODING_DEFAULT, defaultEncoding);
		properties.put(RuntimeConstants.RESOURCE_LOADER, resourceLoader);
		properties.put(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
		properties.put(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, cache);
		properties.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
		properties.put("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		properties.put("file.resource.loader.class", FileResourceLoader.class.getName());
		VelocityEngine engine = new VelocityEngine(properties);
		engine.init();
		return new TemplateManagerImpl(engine);
	}

}
