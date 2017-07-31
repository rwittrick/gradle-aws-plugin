/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.classmethod.aws.gradle.lambda;

import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;

public class AWSLambdaCreateAliasTask extends ConventionTask {
	
	@Getter
	@Setter
	private String functionName;
	
	@Getter
	@Setter
	private String functionVersion;
	
	@Getter
	@Setter
	private String aliasDescription;
	
	@Getter
	@Setter
	private String aliasName;
	
	@Getter
	private CreateAliasResult createAliasResult;
	
	
	public AWSLambdaCreateAliasTask() {
		setDescription("Create Lambda Alias.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void createAlias() throws IOException {
		// to enable conventionMappings feature
		String aliasName = getAliasName();
		String functionName = getAliasName();
		String functionVersion = getAliasName();
		
		if (aliasName == null) {
			throw new GradleException("aliasName is required");
		}
		
		if (functionName == null) {
			throw new GradleException("functionName is required");
		}
		
		if (functionVersion == null) {
			throw new GradleException("functionVersion is required");
		}
		
		AWSLambdaPluginExtension ext = getProject().getExtensions().getByType(AWSLambdaPluginExtension.class);
		AWSLambda lambda = ext.getClient();
		
		CreateAliasRequest request = new CreateAliasRequest()
			.withName(getAliasName())
			.withDescription(getAliasDescription())
			.withFunctionName(getFunctionName())
			.withFunctionVersion(getFunctionVersion());
		createAliasResult = lambda.createAlias(request);
		getLogger().info("Create Lambda alias requested: {}", createAliasResult.getAliasArn());
	}
}
