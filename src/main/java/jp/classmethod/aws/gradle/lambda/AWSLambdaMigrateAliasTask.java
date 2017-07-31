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
import com.amazonaws.services.lambda.model.GetAliasRequest;
import com.amazonaws.services.lambda.model.GetAliasResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;

public class AWSLambdaMigrateAliasTask extends ConventionTask {
	
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
	
	
	public AWSLambdaMigrateAliasTask() {
		setDescription("Create / Update Lambda Alias.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void createOrUpdateAlias() throws IOException {
		// to enable conventionMappings feature
		String aliasName = getAliasName();
		String functionName = getFunctionName();
		
		if (aliasName == null || functionName == null) {
			throw new GradleException("aliasName and functionName are required");
		}
		
		AWSLambdaPluginExtension ext = getProject().getExtensions().getByType(AWSLambdaPluginExtension.class);
		AWSLambda lambda = ext.getClient();
		
		try {
			GetAliasResult getAliasResult =
					lambda.getAlias(new GetAliasRequest().withName(aliasName).withFunctionName(functionName));
			
			updateAlias(lambda, getAliasResult);
			
		} catch (ResourceNotFoundException e) {
			getLogger().warn(e.getMessage());
			getLogger().warn("Creating alias... {}", aliasName);
			createAlias(lambda);
		}
	}
	
	private void createAlias(AWSLambda lambda) throws IOException {
		CreateAliasRequest request = new CreateAliasRequest()
			.withName(getAliasName())
			.withDescription(getAliasDescription())
			.withFunctionName(getFunctionName())
			.withFunctionVersion(getFunctionVersion());
		createAliasResult = lambda.createAlias(request);
		getLogger().info("Create Lambda alias requested: {}", createAliasResult.getAliasArn());
	}
	
	private void updateAlias(AWSLambda lambda, GetAliasResult config) throws IOException {
		// to enable conventionMappings feature
		String updateAliasName = getAliasName();
		if (updateAliasName == null) {
			updateAliasName = config.getName();
		}
		
		String updateAliasDescription = getAliasDescription();
		if (updateAliasDescription == null) {
			updateAliasDescription = config.getDescription();
		}
		
		String updateFunctionVersion = getFunctionVersion();
		if (updateFunctionVersion == null) {
			updateFunctionVersion = config.getFunctionVersion();
		}
		
		UpdateAliasRequest request = new UpdateAliasRequest()
			.withFunctionName(getFunctionName())
			.withFunctionVersion(updateFunctionVersion)
			.withName(updateAliasName)
			.withDescription(updateAliasDescription);
		
		UpdateAliasResult updateAlias = lambda.updateAlias(request);
		getLogger().info("Update Lambda alias update requested: {}",
				updateAlias.getAliasArn());
	}
}
