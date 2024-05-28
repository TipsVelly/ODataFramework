package com.opendev.odata.framework.controller;

import com.opendev.odata.framework.service.CustomActionProcessor;
import com.opendev.odata.framework.service.CustomEntityCollectionProcessor;
import com.opendev.odata.framework.service.CustomEntityProcessor;
import com.opendev.odata.framework.service.CustomPrimitiveProcessor;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.springframework.web.bind.annotation.CrossOrigin;
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

	private final CsdlEdmProvider csdlEdmProvider;

	private final CustomActionProcessor customActionProcessor;

	private final CustomEntityCollectionProcessor customEntityCollectionProcessor;

	private final CustomEntityProcessor customEntityProcessor;

	private final CustomPrimitiveProcessor customPrimitiveProcessor;

	@RequestMapping(value = "*")
	@CrossOrigin(origins = "http://localhost:8080") // specify the allowed origin
	public void process(HttpServletRequest request, HttpServletResponse response) {

		response.setHeader("Access-Control-Allow-Origin", "**");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods", "*");
		response.setHeader("Access-Control-Max-Age", "3600");
		response.setHeader("Access-Control-Allow-Headers",
				"Origin, X-Requested-With, Content-Type, Accept, Authorization");

		OData odata = OData.newInstance();
		ServiceMetadata edm = odata.createServiceMetadata(csdlEdmProvider,
				new ArrayList<>());
		ODataHttpHandler handler = odata.createHandler(edm);
		handler.register(customEntityCollectionProcessor);
		handler.register(customEntityProcessor);
		handler.register(customActionProcessor);
		handler.register(customPrimitiveProcessor);
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
