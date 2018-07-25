package eu.dkvz;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.dkvz.BlogAuthoring.model.*;

@Service
public class ArticlerImportService {

	@Value("${import-path}")
	private String importPath;
	
	private JsonDirImporter jsDirImporter;
	
	@Autowired
    private BlogDataAccessSpring blogDataAccess;
	
	@PostConstruct
	public void initialize() throws Exception {
		this.jsDirImporter = new JsonDirImporter(this.importPath);
		if (!this.jsDirImporter.isWritable()) {
			throw new Exception("FATAL: The import path " + this.importPath + " is not writable.");
		}
	}
	
	
	
}
