package org.hobbit.benchmark.questionanswering.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummySystemAdapter extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummySystemAdapter.class);

    protected NIFParser reader;
    protected NIFWriter writer;
	
    //new
    public DummySystemAdapter() {
    	LOGGER.error("INFO: DummySys Constructor.");
    	reader = new TurtleNIFParser();
		writer = new TurtleNIFWriter();
	}
    
    //new
    @Override
	public void init() throws Exception {
    	LOGGER.error("INFO: DummySys init.");
		super.init();
	}
    
    public void receiveGeneratedData(byte[] data) {
    	LOGGER.error("INFO: Error. DummySys receiveGeneratedData.");
    	LOGGER.warn("Got unexpected data from the data generators.");
	}
	public void receiveGeneratedTask(String taskId, byte[] data) {
		if(Integer.parseInt(taskId)%100==0){
			LOGGER.error("INFO: DummySys receiveGeneratedTask. "+taskId);
		}
		String receivedDataAsString = RabbitMQUtils.readString(data);
		String dummyResult = "dummyResult";
//		List<Document> documents = reader.parseNIF(receivedDataAsString);
//        Document document = documents.get(0);
//        document.setDocumentURI("http://example.org/DummyResponse_" + taskId);
//        LOGGER.info("Sending document " + document.toString());
//        LOGGER.error("INFO: DummySys sending document " + document.toString());
        try {
//        	sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document))));
        	sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(dummyResult));
        } catch (IOException e) {
            LOGGER.error("Got an exception while sending response.", e);
        }
	}
	
	//new 
	@Override
	public void close() throws IOException {
		LOGGER.error("INFO: DummySys close.");
		super.close();
	}
}

