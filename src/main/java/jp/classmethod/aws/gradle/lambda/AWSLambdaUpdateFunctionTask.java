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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;

public class AWSLambdaUpdateFunctionTask extends ConventionTask {
	
	@Getter
	@Setter
	private String functionName;
	
	@Getter
	@Setter
	private String role;
	
	@Getter
	@Setter
	private Runtime runtime;
	
	@Getter
	@Setter
	private String handler;
	
	@Getter
	@Setter
	private String functionDescription;
	
	@Getter
	@Setter
	private Integer timeout;
	
	@Getter
	@Setter
	private Integer memorySize;
	
	@Getter
	@Setter
	private File zipFile;
	
	@Getter
	@Setter
	private S3File s3File;
	
	@Getter
	@Setter
	private VpcConfigWrapper vpc;
	
	@Getter
	@Setter
	private Map<String, String> environment;
	
	@Getter
	@Setter
	private Boolean publish;
	
	
	public AWSLambdaUpdateFunctionTask() {
		setDescription("Update an existing Lambda function.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void updateFunction() throws FileNotFoundException, IOException {
		// to enable conventionMappings feature
		String functionName = getFunctionName();
		File zipFile = getZipFile();
		S3File s3File = getS3File();
		
		if (functionName == null) {
			throw new GradleException("functionName is required");
		}
		
		if ((zipFile == null && s3File == null) || (zipFile != null && s3File != null)) {
			throw new GradleException("exactly one of zipFile or s3File is required");
		}
		if (s3File != null) {
			s3File.validate();
		}
		
		AWSLambdaPluginExtension ext = getProject().getExtensions().getByType(AWSLambdaPluginExtension.class);
		AWSLambda lambda = ext.getClient();
		
		try {
			GetFunctionResult getFunctionResult =
					lambda.getFunction(new GetFunctionRequest().withFunctionName(functionName));
			FunctionConfiguration config = getFunctionResult.getConfiguration();
			if (config == null) {
				config = new FunctionConfiguration().withRuntime(Runtime.Nodejs);
			}
			
			updateFunctionCode(lambda);
			updateFunctionConfiguration(lambda, config);
		} catch (ResourceNotFoundException e) {
			getLogger().warn(e.getMessage());
			getLogger().error("Function does not exist... {}", functionName);
			throw e;
		}
	}
	
	private void updateFunctionCode(AWSLambda lambda) throws IOException {
		// to enable conventionMappings feature
		File zipFile = getZipFile();
		S3File s3File = getS3File();
		Boolean publish = getPublish();
		
		UpdateFunctionCodeRequest request = new UpdateFunctionCodeRequest()
			.withFunctionName(getFunctionName());
		if (zipFile != null) {
			try (RandomAccessFile raf = new RandomAccessFile(getZipFile(), "r");
					FileChannel channel = raf.getChannel()) {
				MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
				buffer.load();
				request = request
					.withZipFile(buffer)
					.withPublish(publish);
			}
		} else {
			// assume s3File is not null
			request = request
				.withS3Bucket(s3File.getBucketName())
				.withS3Key(s3File.getKey())
				.withS3ObjectVersion(s3File.getObjectVersion())
				.withPublish(publish);
		}
		UpdateFunctionCodeResult updateFunctionCode = lambda.updateFunctionCode(request);
		getLogger().info("Update Lambda function requested: {}", updateFunctionCode.getFunctionArn());
	}
	
	private void updateFunctionConfiguration(AWSLambda lambda, FunctionConfiguration config) {
		String updateFunctionName = getFunctionName();
		if (updateFunctionName == null) {
			updateFunctionName = config.getFunctionName();
		}
		
		String updateRole = getRole();
		if (updateRole == null) {
			updateRole = config.getRole();
		}
		
		Runtime updateRuntime = getRuntime();
		if (updateRuntime == null) {
			updateRuntime = Runtime.fromValue(config.getRuntime());
		}
		
		String updateHandler = getHandler();
		if (updateHandler == null) {
			updateHandler = config.getHandler();
		}
		
		String updateDescription = getFunctionDescription();
		if (updateDescription == null) {
			updateDescription = config.getDescription();
		}
		
		Integer updateTimeout = getTimeout();
		if (updateTimeout == null) {
			updateTimeout = config.getTimeout();
		}
		
		Integer updateMemorySize = getMemorySize();
		if (updateMemorySize == null) {
			updateMemorySize = config.getMemorySize();
		}
		
		UpdateFunctionConfigurationRequest request = new UpdateFunctionConfigurationRequest()
			.withFunctionName(updateFunctionName)
			.withRole(updateRole)
			.withRuntime(updateRuntime)
			.withHandler(updateHandler)
			.withDescription(updateDescription)
			.withTimeout(updateTimeout)
			.withVpcConfig(getVpcConfig())
			.withEnvironment(new Environment().withVariables(getEnvironment()))
			.withMemorySize(updateMemorySize);
		
		UpdateFunctionConfigurationResult updateFunctionConfiguration = lambda.updateFunctionConfiguration(request);
		getLogger().info("Update Lambda function configuration requested: {}",
				updateFunctionConfiguration.getFunctionArn());
	}
	
	private VpcConfig getVpcConfig() {
		if (getVpc() != null) {
			return getVpc().toVpcConfig();
		}
		return null;
	}
}
