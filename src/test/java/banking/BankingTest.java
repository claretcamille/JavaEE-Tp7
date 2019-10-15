package banking;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;


public class BankingTest {
	private static DataSource myDataSource; // La source de données à utiliser
	private static Connection myConnection ;	
	private BankingDAO myDAO;
	
	@Before
	public void setUp() throws SQLException, IOException, SqlToolError {
		// On crée la connection vers la base de test "in memory"
		myDataSource = getDataSource();
		myConnection = myDataSource.getConnection();
		// On initialise la base avec le contenu d'un fichier de test
		String sqlFilePath = BankingTest.class.getResource("testdata.sql").getFile();
		SqlFile sqlFile = new SqlFile(new File(sqlFilePath));
		sqlFile.setConnection(myConnection);
		sqlFile.execute();
		sqlFile.closeReader();	
		// On crée l'objet à tester
		myDAO = new BankingDAO(myDataSource);
	}
	
	@After
	public void tearDown() throws SQLException {
		myConnection.close();		
		myDAO = null; // Pas vraiment utile
	}

	
	@Test
	public void findExistingCustomer() throws SQLException {
		float balance = myDAO.balanceForCustomer(0);
		// attention à la comparaison des nombres à virgule flottante !
		// Le dernier paramètre correspond à la marge d'erreur tolérable
		assertEquals("Balance incorrecte !", 100.0f, balance, 0.001f);
	}

	@Test
	public void successfulTransfer() throws Exception {
		float amount = 10.0f;
		int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
		int toCustomer = 1;
		// On mémorise les balances dans les deux comptes avant la transaction
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
		// On exécute la transaction, qui doit réussir
		myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
		// Les balances doivent avoir été mises à jour dans les 2 comptes
		assertEquals("Balance incorrecte !", before0 - amount, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("Balance incorrecte !", before1 + amount, myDAO.balanceForCustomer(toCustomer), 0.001f);				
	}
        
                      /*
                                Q1 : Si lors de la transaction le solde du compte débité devient négatif
                                Dans ce cas la on essaye de trop débité, donc le débit est impossible et rien ne change.
                      */
                      
                     @Test
                     public void transactionRefuser() throws Exception {
                                            float amount = 10.0f;
		int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
		int toCustomer = 1;
		// On mémorise les balances dans les deux comptes avant la transaction
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
                                            try{
                                                                 // Ici on effectue un débit trop important
                                                                 myDAO.bankTransferTransaction(fromCustomer, toCustomer, before0 + amount);
                                                                 fail();// Ici exception attendu
                                            }catch (Exception ex){
                                                                 // Vérification que rien n'a bouger 
                                                                 assertEquals("Balance incorrecte !", before0 , myDAO.balanceForCustomer(fromCustomer), 0.001f);
                                                                 assertEquals("Balance incorrecte !", before1 , myDAO.balanceForCustomer(toCustomer), 0.001f);
                                            }
                     }
                     
                     /*
                                 Q2 : Si le compte débité ou le compte crédité n'existent pas
                                Ici rien ne change en cas de crédit ou de débit chez un client inconue
                     */
                     
                     @Test
                     public void creditInconnue() throws Exception{
                                            float amount = 10.0f;
                                            int toCustomer = 0; // Le client existe dans la DAO
		int fromCustomer = 100; // client qui n'existe pas dans la DAO
                                            float before = myDAO.balanceForCustomer(toCustomer);// Mémorisation de la balance du client existant
                                            try{
                                                                 myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                                                                 fail();// Ici exception attendu
                                            }catch (Exception ex){
                                                                  // Vérification qu'aucune transavtion n'a eu lieu
                                                                 assertEquals("Balance incorrecte !", before , myDAO.balanceForCustomer(toCustomer), 0.001f);
                                            }
                     }
                    
                     @Test
                     public void debitInconnue() throws Exception{
                                           float amount = 10.0f;
                                            int toCustomer = 100; // Le client n'existe pas dans la DAO
		int fromCustomer = 0; // client qui existe dans la DAO
                                            float before = myDAO.balanceForCustomer(fromCustomer);// Mémorisation de la balance du client existant
                                            try{
                                                                  myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                                                                 fail();// Ici exception attendu
                                            }catch (Exception ex){
                                                                  // Vérification qu'aucune transavtion n'a eu lieu
                                                                 assertEquals("Balance incorrecte !", before , myDAO.balanceForCustomer(fromCustomer), 0.001f);
                                            }
                     }
	

	public static DataSource getDataSource() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource ds = new org.hsqldb.jdbc.JDBCDataSource();
		ds.setDatabase("jdbc:hsqldb:mem:testcase;shutdown=true");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}	
}
