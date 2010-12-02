package liquibase.dsl.parser.groovy
//
//    This file is part of Liquibase-DSL.
//
//    Liquibase-DSL is free software: you can redistribute it and/or modify
//    it under the terms of the GNU Lesser General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Liquibase-DSL is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public License
//    along with Liquibase-DSL.  If not, see <http://www.gnu.org/licenses/>.
//
import java.util.LinkedList;

import grails.util.Environment
import liquibase.*
import liquibase.parser.*
import liquibase.exception.*
import liquibase.database.Database
import org.apache.log4j.*

// added by jun Chen
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication


/**
*		Provides access to the properties set in the directory denoted by the "lbdsl.home" property.
*/
class GroovyChangeLogParser implements ChangeLogParserImpl {

	// added by jun Chen
	private dbChangeLog
    
    List<Class> migrationClasses
    
    def log = Logger.getLogger(GroovyChangeLogParser)
	
	public DatabaseChangeLog parse(String physicalChangeLogLocation, 
                                   FileOpener fileOpener, 
                                   Map changeLogProperties, Database db, 
                                   DefaultGrailsApplication app) {
								
								
	    	/**  modified by jun chen
		      *  The file "changelog.groovy" is not nessasary any more.
		      */
								   						   
//		if(!fileOpener) {
//			throw new IllegalArgumentException("Need to specify a fileOpener")
//		}
		
		
		
		dbChangeLog = new GroovyDatabaseChangeLog(physicalChangeLogLocation, db);
		
		// modified by jun chen
        //dbChangeLog.fileOpener = fileOpener;
		dbChangeLog.grailsEnv = Environment.current.name

        this.migrationClasses = app.MigrationClasses
        
        log.debug("Running the following migration classes in this order: " + getSortedMigrations())
        
		getSortedMigrations().each{  
    		if (it.migration) {
                parse it.migration
    		}
		}
            
        return (DatabaseChangeLog)dbChangeLog;
	}  
		    

	void parse(Closure migration) {
		migration.delegate = dbChangeLog
		migration.call()
	}
    
    List<Class> getSortedMigrations() {
        def result = new LinkedList<Class>()
        migrationClasses.each { migration ->
            def instance = migration.originalClass.newInstance()
            if(migration.originalClass.metaClass.hasProperty(instance, "runAfter") &&
               instance.runAfter?.size() > 0) {
               int lastRunAfterIndex = Math.max(0,instance.runAfter.collect({ result.originalClass.indexOf(it) + 1 }).max())
               result.add(lastRunAfterIndex, migration)
               
               // Now we must reorder the table in order to take into account 
               // the new element 
               for (i in lastRunAfterIndex..0) {
                   def currentInstance = result[i].originalClass.newInstance()
                   if(currentInstance.metaClass.hasProperty(currentInstance, "runAfter") &&
                      currentInstance.runAfter?.contains(migration.originalClass)) {
                          def movingMigration = result[i]
                          result -= movingMigration
                         // result.add(lastRunAfterIndex + 1, movingMigration) 
						  if (lastRunAfterIndex + 1 < result.size())
						  result.add(lastRunAfterIndex + 1, movingMigration)
					      else
						  result << movingMigration

						  }
               }
            }
            else {
                result.add(0,migration) 
            }
        }
        return result
    }
}
