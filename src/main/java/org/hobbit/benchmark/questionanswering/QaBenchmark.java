package org.hobbit.benchmark.questionanswering;

import java.io.IOException;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.hobbit.core.Commands;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QaBenchmark extends AbstractBenchmarkController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QaBenchmark.class);
	
	private static final String DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/cmartens/qadatagenerator";
	private static final String TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/cmartens/qataskgenerator";
	private static final String EVALUATION_MODULE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/conrads/qaevaluationmodule";
	private static final String EVALUATION_STORE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/defaulthobbituser/defaultevaluationstorage";
	//TODO private static final String EVALUATION_STORE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/?/?";
	
	protected static final String gerbilUri = "http://w3id.org/gerbil/vocab#";
	protected static final String gerbilQaUri = "http://w3id.org/gerbil/qa/hobbit/vocab#";
	protected static final Resource QA = resource("QA");
	private ExperimentType experimentType;
	private String experimentTypeName;
	private String experimentTaskName;
	private String questionLanguage;
	private String sparqlService;
	private int numberOfDocuments;
	private long seed;
	
	//create single data and task generator
	private int numberOfGenerators = 1;
	
	protected static final Resource resource(String local) {
        return ResourceFactory.createResource(gerbilUri + local);
    }

    @Override
    public void init() throws Exception {
    	LOGGER.info("Initializing.");
    	super.init();
    	LOGGER.info("result model test: "+this.resultModel);
        
    	//load experimentType from benchmark model
        NodeIterator iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(gerbilQaUri+"hasExperimentType"));
        if (iterator.hasNext()) {
            try {
            	Resource resource = iterator.next().asResource();
            	if (resource == null) { LOGGER.error("Got null resource."); }
            	String uri = resource.getURI();
            	if (QA.getURI().equals(uri)) {
                    experimentType = ExperimentType.QA;
                }
                experimentTypeName = experimentType.getName();
                LOGGER.info("Got experiment type from the parameter model: \""+experimentTypeName+"\"");
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        //check experimentType
        if(!experimentType.equals(ExperimentType.QA)){
        	String msg = "Couldn't get valid experiment type from the parameter model. Must be "+ExperimentType.QA.getName()+". Aborting.";
        	LOGGER.error(msg);
        	throw new Exception(msg);
        }
        
        //load experimentTaskName from benchmark model
        experimentTaskName = "";
        iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(gerbilQaUri+"hasExperimentTask"));
        if (iterator.hasNext()) {
            try {
            	experimentTaskName = iterator.next().asLiteral().getString().toLowerCase();
                LOGGER.info("Got experiment task from the parameter model: \""+experimentTaskName+"\"");
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        //check experimentTaskName
        if(!experimentTaskName.equalsIgnoreCase("hybrid") && !experimentTaskName.equalsIgnoreCase("largescale") && !experimentTaskName.equalsIgnoreCase("multilingual")) {
        	String msg = "Couldn't get the experiment task from the parameter model. Must be \"hybrid\", \"largescale\" or \"multilingual\". Aborting.";
        	LOGGER.error(msg);
        	throw new Exception(msg);
        }
        
        //load questionLanguage from benchmark model
        questionLanguage = "";
        if (!experimentTaskName.equalsIgnoreCase("multilingual")) {
        	questionLanguage = "en";
        	LOGGER.info("Setting question language to \"en\" due to experiment type is not \"multilingual\".");
        }else{
        	iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(gerbilQaUri+"hasQuestionLanguage"));
            if (iterator.hasNext()) {
                try {
                	questionLanguage = iterator.next().asLiteral().getString();
                	LOGGER.info("Got language from the parameter model: \""+questionLanguage+"\"");
                } catch (Exception e) {
                    LOGGER.error("Exception while parsing parameter.", e);
                }
            }
        }
        //check questionLanguage
        if(!questionLanguage.equalsIgnoreCase("en") && !questionLanguage.equalsIgnoreCase("fa") && !questionLanguage.equalsIgnoreCase("de") && !questionLanguage.equalsIgnoreCase("es") 
    			&& !questionLanguage.equalsIgnoreCase("it") && !questionLanguage.equalsIgnoreCase("fr") && !questionLanguage.equalsIgnoreCase("nl") && !questionLanguage.equalsIgnoreCase("ro")){
    		LOGGER.error("Couldn't get the right language from the parameter model. Must be one of: \"en\", \"fa\", \"de\", \"es\", \"it\", \"fr\", \"nl\", \"ro\". Using default value.");
    		questionLanguage = "en";
    		LOGGER.info("Setting language to default value: \"en\"");
    	}
        
        //load numberOfDocuments from benchmark model
        numberOfDocuments = -1;
        iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(gerbilQaUri+"hasNumberOfDocuments"));
        if(iterator.hasNext()) {
        	try {
        		numberOfDocuments = iterator.next().asLiteral().getInt();
                LOGGER.info("Got number of documents from the parameter model: \""+numberOfDocuments+"\"");
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        //check numberOfDocuments
        if (numberOfDocuments < 0) {
        	LOGGER.error("Couldn't get the number of documents from the parameter model. Using default value for type \""+experimentType+"\".");
        	if(!experimentType.equals("largescale")){
        		numberOfDocuments = 50;
        	}else{ numberOfDocuments = 500; }
        	LOGGER.info("Setting number of documents to default value: \""+numberOfDocuments+"\"");
        }
        
        //load seed from benchmark model
        seed = -1;
        iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty("http://w3id.org/gerbil/qa/hobbit/vocab#hasSeed"));
        if(iterator.hasNext()) {
        	try {
        		seed = iterator.next().asLiteral().getLong();
                LOGGER.info("Got seed from the parameter model: \""+seed+"\"");
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        //check seed
        if (seed < 0) {
        	LOGGER.error("Couldn't get the seed from the parameter model. Using default value.");
        	seed = 42;
        	LOGGER.info("Setting seed to default value: \""+seed+"\"");
        }
        
        //load SparqlService from benchmark model
        sparqlService = "";
    	iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(gerbilQaUri+"hasSparqlService"));
        if (iterator.hasNext()) {
            try {
            	sparqlService = iterator.next().asLiteral().getString();
            	LOGGER.info("Got SPARQL service from the parameter model: \""+sparqlService+"\"");
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        //check SparqlService
        try{
		    String query = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> ask where { dbr:DBpedia dbo:license dbr:GNU_General_Public_License . }";
		    QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlService, query);
		    qexec.execAsk();
        }catch(Exception e){
        	String msg = "SPARQL service can not be accessed. Aborting.";
        	LOGGER.error(msg, e);
        	throw new Exception(msg, e);
        }

        //create data generator
        LOGGER.info("Creating Data Generator.");
        String[] envVariables = new String[]{
        		QaDataGenerator.EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.getName(),
        		QaDataGenerator.EXPERIMENT_TASK_PARAMETER_KEY + "=" + experimentTaskName,
        		QaDataGenerator.QUESTION_LANGUAGE_PARAMETER_KEY + "=" + questionLanguage,
                QaDataGenerator.NUMBER_OF_DOCUMENTS_PARAMETER_KEY + "=" + numberOfDocuments,
                QaDataGenerator.SEED_PARAMETER_KEY + "=" + seed,
                QaDataGenerator.SPARQL_SERVICE_PARAMETER_KEY + "=" + sparqlService,};
        createDataGenerators(DATA_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, envVariables);

        //create task generator
        LOGGER.info("Creating Task Generator.");
        envVariables = new String[] {
        		QaDataGenerator.EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.name(),
        		QaDataGenerator.EXPERIMENT_TASK_PARAMETER_KEY + "=" + experimentTaskName,
        		QaTaskGenerator.QUESTION_LANGUAGE_PARAMETER_KEY + "=" + questionLanguage,
				QaTaskGenerator.SEED_PARAMETER_KEY + "=" + seed};
        createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, envVariables);

        //create evaluation storage
        LOGGER.info("Creating Evaluation Storage.");
        createEvaluationStorage(EVALUATION_STORE_CONTAINER_IMAGE, new String[] { "HOBBIT_RABBIT_HOST="+ connection.getAddress().toString() });

        //wait for all components to finish their initialization
        LOGGER.info("Waiting for components to finish their initialization.");
        waitForComponentsToInitialize();
        
        LOGGER.info("Initialized.");
    }
	
	@Override
    protected void executeBenchmark() throws Exception {
		LOGGER.info("Executing benchmark.");

        sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
        sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);

        LOGGER.info("Waiting for Generators and System to finish.");
        waitForDataGenToFinish();
        waitForTaskGenToFinish();
        waitForSystemToFinish();
        
        LOGGER.info("Creating Evaluation Module and waiting for evaluation components to finish.");
        //FIXME? GerbilEvaluationModule.EXPERIMENT_TYPE_KEY = "qa.experiment_type";
        createEvaluationModule(EVALUATION_MODULE_CONTAINER_IMAGE, new String[] { "qa.experiment_type" + "=" + experimentType.name() });
        waitForEvalComponentsToFinish();
        
        LOGGER.info("Sending result model.");
        sendResultModel(this.resultModel);
        
        LOGGER.info("Benchmark executed.");
    }
	
	@Override
    public void close() throws IOException {
		LOGGER.info("Closing.");
        super.close();
        LOGGER.info("Closed.");
    }
}