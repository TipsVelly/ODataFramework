package com.opendev.odata.framework.controller;

import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * @author Subash
 * @since 2/28/2021
 */
@RestController
@RequestMapping(ODataController.URI)
@RequiredArgsConstructor
public class ODataController {
	
	protected static final String URI = "/OData/V1.0";

	
	private final CsdlEdmProvider edmProvider;
	
	
	private final EntityCollectionProcessor processor;
	
	private final EntityProcessor  entityProcessor;

	@RequestMapping(value = "*")
	public void process(HttpServletRequest request, HttpServletResponse response) {
		OData odata = OData.newInstance();
		ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
				new ArrayList<>());
		ODataHttpHandler handler = odata.createHandler(edm);
		handler.register(processor);
		handler.register(entityProcessor);
		handler.process(new HttpServletRequestWrapper(request) {
			// Spring MVC matches the whole path as the servlet path
			// Olingo wants just the prefix, ie upto /OData/V1.0, so that it
			// can parse the rest of it as an OData path. So we need to override
			// getServletPath()
			@Override
			public String getServletPath() {
				return ODataController.URI;
			}
		}, response);
	}
}
