package hci.gnomex.security.tomcat;

import hci.gnomex.security.ActiveDirectory;
import hci.gnomex.security.EncrypterService;
import hci.gnomex.security.EncryptionUtility;
import hci.gnomex.utility.Util;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.sql.DataSource;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.log4j.Logger;

public class GNomExLDAPRealm extends RealmBase {

	private static final Logger LOG = Logger.getLogger(GNomExLDAPRealm.class);

	private String username;
	private String password;

	private String ldap_init_context_factory;
	private String ldap_sec_principal;
	private String ldap_provider_url;
	private String ldap_protocol;
	private String ldap_auth_meth;
	private String ldap_domain = null;
	private String ldap_user_attributes = null;
	private HashMap<String, String> ldap_user_attribute_map = new HashMap<String, String>();

	private String alt_ldap_init_context_factory;
	private String alt_ldap_provider_url;
	private String alt_ldap_auth_meth;
	private String alt_ldap_base_dn;
	private String alt_ldap_user_name;
	private String alt_ldap_password;

	private String datasource_lookup_name;

	private Hashtable env1 = new Hashtable();
	private DirContext ctx;
	private Attributes attr;

	public GNomExLDAPRealm() {
		super();
	}

	@Override
	public Principal authenticate(String username, String credentials) {
		LOG.debug("Authenticating user: " + username);
		this.username = username;
		this.password = credentials;

		if (isAuthenticated()) {
			return getPrincipal(username);
		} else {
			return null;
		}
	}

	@Override
	protected Principal getPrincipal(String username) {
		List<String> roles = new ArrayList<String>();
		roles.add("GNomExUser");
		return new GenericPrincipal(username, password, roles);
	}

	@Override
	protected String getPassword(String string) {
		return password;
	}

	@Override
	protected String getName() {
		return this.getClass().getSimpleName();
	}

	public String getLdap_provider_url() {
		return ldap_provider_url;
	}

	public void setLdap_provider_url(String ldap_provider_url) {
		this.ldap_provider_url = ldap_provider_url;
	}

	public String getLdap_init_context_factory() {
		return ldap_init_context_factory;
	}

	public void setLdap_init_context_factory(String ldap_init_context_factory) {
		this.ldap_init_context_factory = ldap_init_context_factory;
	}

	public String getAlt_ldap_provider_url() {
		return alt_ldap_provider_url;
	}

	public void setAlt_ldap_provider_url(String alt_ldap_provider_url) {
		this.alt_ldap_provider_url = alt_ldap_provider_url;
	}

	public String getAlt_ldap_base_dn() {
		return alt_ldap_base_dn;
	}

	public void setAlt_ldap_base_dn(String alt_ldap_base_dn) {
		this.alt_ldap_base_dn = alt_ldap_base_dn;
	}

	public String getAlt_ldap_user_name() {
		return alt_ldap_user_name;
	}

	public void setAlt_ldap_user_name(String alt_ldap_user_name) {
		this.alt_ldap_user_name = alt_ldap_user_name;
	}

	public String getAlt_ldap_password() {
		return alt_ldap_password;
	}

	public void setAlt_ldap_password(String alt_ldap_password) {
		this.alt_ldap_password = alt_ldap_password;
	}

	public String getAlt_ldap_init_context_factory() {
		return alt_ldap_init_context_factory;
	}

	public void setAlt_ldap_init_context_factory(String alt_ldap_init_context_factory) {
		this.alt_ldap_init_context_factory = alt_ldap_init_context_factory;
	}

	public String getLdap_protocol() {
		return ldap_protocol;
	}

	public void setLdap_protocol(String ldap_protocol) {
		this.ldap_protocol = ldap_protocol;
	}

	public String getLdap_auth_meth() {
		return ldap_auth_meth;
	}

	public void setLdap_auth_meth(String ldap_auth_meth) {
		this.ldap_auth_meth = ldap_auth_meth;
	}

	public String getAlt_ldap_auth_meth() {
		return alt_ldap_auth_meth;
	}

	public void setAlt_ldap_auth_meth(String alt_ldap_auth_meth) {
		this.alt_ldap_auth_meth = alt_ldap_auth_meth;
	}

	public String getLdap_domain() {
		return ldap_domain;
	}

	public void setLdap_domain(String ldap_domain) {
		this.ldap_domain = ldap_domain;
	}

	public String getLdap_user_attributes() {
		return ldap_user_attributes;
	}

	public void setLdap_user_attributes(String ldap_user_attributes) {
		LOG.debug("Setting LDAP user attributes: " + ldap_user_attributes);
		this.ldap_user_attributes = ldap_user_attributes;

		// Populate user attributes and values into a may (key=attribute, value=attribute value)
		if (ldap_user_attributes != null && ldap_user_attributes.length() > 0) {
			String attributeTokens[] = ldap_user_attributes.split(",");
			for (int x = 0; x < attributeTokens.length; x++) {
				String tokens[] = attributeTokens[x].split(":");
				String attribute = tokens.length > 0 ? tokens[0] : null;
				String value = tokens.length > 1 ? tokens[1] : null;
				if (attribute != null && value != null) {
					ldap_user_attribute_map.put(attribute, value);
				} else {
					System.out.println("Unexpected token for ldap_user_attributes: " + " attribute entry=" + attributeTokens[x] + " tokens=" + tokens);
				}
			}
		}

	}

	public String getLdap_sec_principal() {
		return ldap_sec_principal;
	}

	public void setLdap_sec_principal(String ldap_sec_principal) {
		this.ldap_sec_principal = ldap_sec_principal;
	}

	public String getDatasource_lookup_name() {
		return datasource_lookup_name;
	}

	public void setDatasource_lookup_name(String datasource_lookup_name) {
		this.datasource_lookup_name = datasource_lookup_name;
	}

	private boolean isAuthenticated() {
		boolean isAuthenticated = false;

		if (this.isGNomExUniversityUser()) {
			LOG.debug("GnomEx University User: "+username);
			// If this is a GNomEx user with a uNID, check the credentials
			// against the Univ of Utah LDAP. If not found there,
			// try HCI directory services.
			if (this.ldap_provider_url != null && !this.ldap_provider_url.equals("")) {
				isAuthenticated = checkLDAPCredentials();
			}
			if (!isAuthenticated) {
				if (this.alt_ldap_provider_url != null && !this.alt_ldap_provider_url.equals("")) {
					isAuthenticated = this.checkAlternateLDAPCredentials();
				}
			}
		} else if (this.isAuthenticatedGNomExExternalUser()) {
			// If this is a GNomEx external user, check credentials
			// against the GNomEx encrypted password
			isAuthenticated = true;
		} else {
			// Otherwise, if this is not a GNomEx user, check the credentials
			// against the Univ of Utah LDAP
			isAuthenticated = checkLDAPCredentials();
		}
		return isAuthenticated;
	}

	private boolean checkLDAPCredentials() {
		LOG.debug("Checking LDAP credentials for user: "+ username);

		if (username == null || password == null || ldap_provider_url == null || ldap_provider_url.length() == 0) {
			return false;
		}

		boolean isAuthenticated = false;

		// Change local copy since GNomExLDAPRealm is apparently static in tomcat
		String localPrincipal = ldap_sec_principal;
		if (localPrincipal != null && localPrincipal.contains("<")) {
			localPrincipal = localPrincipal.replace("<uid>", username);
		} else if (localPrincipal != null && localPrincipal.contains("[")) {
			// Need brackets if provided in a property because <> messes up parsing of context (xml) file
			localPrincipal = localPrincipal.replace("[uid]", username);
		}

		try {
			ActiveDirectory ad = new ActiveDirectory(username, password, ldap_init_context_factory, ldap_provider_url, ldap_protocol, ldap_auth_meth,
					localPrincipal);

			// If user attributes are property is present, then check the user attributes
			// to see if they match the expected value.
			if (ldap_domain != null && ldap_user_attribute_map != null && !ldap_user_attribute_map.isEmpty()) {
				NamingEnumeration<SearchResult> answer = ad.searchUser(username, ldap_domain, Util.keysToArray(ldap_user_attribute_map));
				isAuthenticated = ad.doesMatchUserAttribute(answer, ldap_user_attribute_map);

			} else {
				// If no user attributes property present, we have passed authentication at this point.
				isAuthenticated = true;
			}

		} catch (Exception e) {
			LOG.error("ERROR in checkLDAPCredentials: "+ e.getMessage(),e);
			isAuthenticated = false;
		}
		return isAuthenticated;

	}

	private boolean checkAlternateLDAPCredentials() {
		env1 = new Hashtable();
		env1.put(Context.INITIAL_CONTEXT_FACTORY, alt_ldap_init_context_factory);
		env1.put(Context.SECURITY_AUTHENTICATION, alt_ldap_auth_meth);

		String ldapDNString = "cn=" + alt_ldap_user_name + "," + alt_ldap_base_dn;
		env1.put(Context.PROVIDER_URL, alt_ldap_provider_url);
		env1.put(Context.SECURITY_PRINCIPAL, ldapDNString);
		env1.put(Context.SECURITY_CREDENTIALS, alt_ldap_password);

		// Create the initial directory context
		try {
			ctx = new InitialDirContext(env1);
			attr = new BasicAttributes(true);
		} catch (NamingException e) {
			System.out.println("Problem getting attribute: " + e);
			return false;
		}

		String cn = null;
		try {

			attr.put(new BasicAttribute("sAMAccountName", username));

			// search for username
			NamingEnumeration ne = ctx.search("", attr);
			while (ne.hasMoreElements()) {
				SearchResult sr = (SearchResult) ne.next();

				// get common name for authentication attempt
				cn = sr.getName();
			}

			// did we find our user name ? if not, return null
			if (cn == null) {
				return false;
			}
		} catch (NamingException e) {
			System.out.println("Problem getting attribute: " + e);
			return false;
		}

		// now we have our common name, attempt to bind to AD with password
		String dn = cn + "," + alt_ldap_base_dn;

		env1.put(Context.SECURITY_PRINCIPAL, dn);
		env1.put(Context.SECURITY_CREDENTIALS, password);

		try {
			// Bind to the LDAP directory
			ctx = new InitialDirContext(env1);

			// if we are here, it worked !
			return true;
		} catch (AuthenticationException ae) {
			// wrong password
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean isGNomExUniversityUser() {

		LOG.debug("Checking if "+username+" is a GnomEx U of U user.");
		boolean isGNomExUser = false;

		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			con = this.getConnection();
			stmt = con.prepareStatement("SELECT isActive, uNID FROM AppUser WHERE uNID = ?");
			stmt.setString(1, username);
			rs = stmt.executeQuery();

			while (rs.next()) {
				String isActive = rs.getString("isActive");
				if (isActive != null && isActive.equalsIgnoreCase("Y"))
					isGNomExUser = true;
			}

		} catch (NamingException ne) {
			System.out.println("FATAL: Naming exception while trying to get connection \n" + ne.getMessage());
			return false;
		} catch (ClassNotFoundException cnfe) {
			System.out.println("FATAL: The JDBC driver was not found on the classpath \n" + cnfe.getMessage());
			return false;
		} catch (SQLException ex) {
			System.out.println("FATAL: Unable to run AppUser query hci.gnomex.security.tomcat.GNomExLDAPRealm");
			return false;
		} finally {
			this.closeConnection(con);
		}

		return isGNomExUser;
	}

	private boolean isAuthenticatedGNomExExternalUser() {

		LOG.debug("Checking if "+username+" is an authenticated external GnomEx user.");
		boolean isAuthenticated = false;

		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		EncryptionUtility passwordEncrypter = new EncryptionUtility();

		try {
			con = this.getConnection();
			stmt = con.prepareStatement("SELECT isActive, userNameExternal, passwordExternal, salt FROM AppUser WHERE userNameExternal = ?");
			stmt.setString(1, username);

			rs = stmt.executeQuery();

			while (rs.next()) {
				String isActive = rs.getString("isActive");
				String gnomexPasswordEncrypted = rs.getString("passwordExternal");
				String salt = rs.getString("salt");
				String thePasswordEncryptedNew = "";

				// Uncomment this conditional if you want to prevent inactive users from logging in
				// if (isActive != null && isActive.equalsIgnoreCase("Y")) {
				if (salt != null) {
					thePasswordEncryptedNew = passwordEncrypter.createPassword(password, salt);
				}
				String thePasswordEncryptedOld = EncrypterService.getInstance().encrypt(password);
				if (thePasswordEncryptedNew.equals(gnomexPasswordEncrypted)) {
					isAuthenticated = true;
				} else if (thePasswordEncryptedOld.equals(gnomexPasswordEncrypted)) {
					isAuthenticated = true;
				}
				// }
			}

		} catch (NamingException ne) {
			System.out.println("FATAL: Naming exception while trying to get connection \n" + ne.getMessage());
			return false;
		} catch (ClassNotFoundException cnfe) {
			System.out.println("FATAL: The JDBC driver was not found on the classpath \n" + cnfe.getMessage());
			return false;
		} catch (SQLException ex) {
			System.out.println("FATAL: Unable to initialize hci.gnomex.security.tomcat.SecurityManagerLocal");
			System.out.println(ex.toString());
			return false;
		} finally {
			this.closeConnection(con);
		}

		return isAuthenticated;
	}

	protected Connection getConnection() throws SQLException, ClassNotFoundException, NamingException {
		Context initCtx = new InitialContext();
		DataSource ds = (DataSource) initCtx.lookup(datasource_lookup_name);
		return ds.getConnection();
	}

	protected void closeConnection(Connection con) {
		try {
			if (con != null && !con.isClosed()) {
				con.close();
			}
		} catch (SQLException ex) {
			System.out.println("FATAL: Unable to close db connection in hci.gnomex.security.tomcat.GNomExLDAPRealm");
		}
	}
}
