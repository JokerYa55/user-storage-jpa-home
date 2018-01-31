package keycloak.storage.user;

import DAO.UserAdapter;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import keycloak.bean.UserEntity;
import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.genSalt;
import static keycloak.storage.util.hashUtil.sha1;
//import static keycloak.storage.util.hashUtil.encodeToHex;
import static keycloak.storage.util.hashUtil.md5;
//import static keycloak.storage.util.hashUtil.sha1;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

/**
 * @version 1
 */
@Stateful
@Local(EjbExampleUserStorageProvider.class)
public class EjbExampleUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache,
        UserFederatedStorageProvider {

    private static final Logger log = Logger.getLogger(EjbExampleUserStorageProvider.class);

    /**
     *
     */
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";
    public static final String SALT_CACHE_KEY = UserAdapter.class.getName() + ".salt";
    public static final int SECRET_QUESTION_SIZE = 6;

    /**
     *
     */
    @PersistenceContext(unitName = "user-storage-jpa-example")
    protected EntityManager em;

    /**
     *
     */
    protected ComponentModel model;

    /**
     *
     */
    protected KeycloakSession session;

    /**
     *
     * @param model
     */
    public void setModel(ComponentModel model) {
        this.model = model;
    }

    /**
     *
     * @param session
     */
    public void setSession(KeycloakSession session) {
        this.session = session;
    }

    /**
     *
     * @param realm
     */
    @Override
    public void preRemove(RealmModel realm) {
        log.info("preRemove 1");
    }

    /**
     *
     * @param realm
     * @param group
     */
    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        log.info("preRemove 2");
    }

    /**
     *
     * @param realm
     * @param role
     */
    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        log.info("preRemove 3");
    }

    /**
     *
     */
    @Remove
    @Override
    public void close() {
        log.debug("close");
    }

    /**
     *
     * @param id
     * @param realm
     * @return В случае смены типа поля ID нужно внести изменения
     */
    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        log.info("getUserById\n\n\tid = " + id + "\n\t realm = " + realm.getName());
        log.debug("Get EXT ID = >");
        log.debug("Get EXT ID = > " + StorageId.externalId(id));
        //TODO: В случае смены типа поля ID нужно внести изменения
        Long persistenceId = new Long(StorageId.externalId(id));

        log.debug("persistenceId => " + persistenceId);

        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) {
            log.info("could not find user by id: " + id);
            return null;
        } else {
            log.debug("ID => " + entity.getId().toString());
        }
        return new UserAdapter(session, realm, model, entity, em);
    }

    /**
     *
     * @param username
     * @param realm
     * @return
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        log.info("getUserByUsername: " + username);
        TypedQuery<UserEntity> query = null;
        if (!username.contains("+7")) {
            log.debug("FIND BY USERNAME");
            query = em.createNamedQuery("getUserByUsername", UserEntity.class);
            query.setParameter("username", username);
        } else {
            log.debug("FIND BY PHONE => " + username.substring(2));
            query = em.createNamedQuery("getUserByPhone", UserEntity.class);
            query.setParameter("phone", username.substring(1));
        }

        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) {
            log.info("could not find username: " + username);
            return null;
        }
        return new UserAdapter(session, realm, model, result.get(0), em);
    }

    /**
     *
     * @param email
     * @param realm
     * @return
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        log.info("getUserByEmail");
        TypedQuery<UserEntity> query = em.createNamedQuery("getUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        List<UserEntity> result = query.getResultList();
        if (result.isEmpty()) {
            return null;
        }
        return new UserAdapter(session, realm, model, result.get(0), em);
    }

    /**
     *
     * @param realm
     * @param username
     * @return
     */
    //TODO: 07/01/2018  Исправлено 
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        UserEntity entity = new UserEntity();
        //entity.setId(UUID.randomUUID().toString());
        entity.setUsername(username);
        entity.setUser_status(0);
        entity.setCreate_date(new Date());
        em.persist(entity);
//        UsersLog lUser = new UsersLog();
//        lUser.setUsername(username);
//        lUser.setUserId(entity.getId());
//        lUser.setOperType("I");
//                
//        em.persist(lUser);
        log.info("added user: " + username);

        //UserCredentialModel input = new UserCredentialModel();
        //input.setType("SECRET_QUESTION");
        //String answer = genSalt().substring(0, 6);
        //Math.round(Math.random() * 1000000) + "";
        //input.setValue(answer);
        UserAdapter user = new UserAdapter(session, realm, model, entity, em);
        //user.setSecretQuestion(answer);

        //session.userCredentialManager().updateCredential(realm, (UserModel) user, input);
        return user;
    }

    /**
     * Удаляет пользователя из БД
     *
     * @param realm
     * @param user
     * @return
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        log.info("removeUser");
        //String persistenceId = StorageId.externalId(user.getId());
        Long persistenceId = new Long(StorageId.externalId(user.getId()));
        UserEntity entity = em.find(UserEntity.class, persistenceId);
        if (entity == null) {
            return false;
        }
        //em.remove(entity);
        entity.setUser_status(1);
        entity.setEmail(null);
        entity.setPhone(null);
        em.merge(entity);
        return true;
    }

    /**
     * Добавляются данные пользователя в CACHE (password и salt)
     *
     * @param realm
     * @param user
     * @param delegate
     */
    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        log.info("onCache\n\n\trealm = " + realm.getName() + "\n\tuser = " + user.getUsername() + "\n\tdelegate = " + delegate.getUsername());
        String password = ((UserAdapter) delegate).getPassword();
        String salt = ((UserAdapter) delegate).getSalt();
        log.info("PASSWORD = " + password);
        log.info("PASSWORD_CACHE_KEY = " + PASSWORD_CACHE_KEY);
        if (password != null) {
            log.info("Add PASSWORD in CACHE password = " + password);
            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }

        log.info("SALT_CACHE_KEY = " + SALT_CACHE_KEY);
        if (salt != null) {
            log.info("Add SALT in CAHE salt = " + salt);
            user.getCachedWith().put(SALT_CACHE_KEY, salt);
        }
    }

    /**
     *
     * @param credentialType
     * @return
     */
    @Override
    public boolean supportsCredentialType(String credentialType) {
        log.info("supportsCredentialType\n\n\tcredentialType = " + credentialType);
        CredentialModel.TOTP.equals(credentialType);
        return CredentialModel.PASSWORD.equals(credentialType);
    }

    /**
     * Вызывается при редактировании учетных данных пользователя
     *
     * @param realm
     * @param user
     * @param input
     * @return false - тогда учитываются ограничения пароля установленные в KK
     * true - не учитываются
     */
    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        log.info("updateCredential \n\n\trealm = " + realm.getName() + "\n\tuser = " + user.getUsername() + "\n\tinput = " + input.getClass().getName());
        log.info("input.getType() => " + input.getType());

        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel) input;        
        UserAdapter adapter = getUserAdapter(user);
        log.info("cred.getValue() = > " + cred.getValue() + "\ncred.getType() => " + cred.getType());
        /**
         * TODO: Устанавливаем пароль. В адаптер передается незашифрованный
         * пароль и далее в методе setPassword получается hesh пароля и
         * записывается в БД. В поле password_not_hash записывается пароль
         * введенный пользователем
         */
        adapter.setPassword(cred.getValue());
        return false;
    }

    /**
     *
     * @param rm
     * @param string
     * @param cm
     */
    @Override
    public void updateCredential(RealmModel rm, String string, CredentialModel cm) {
        log.info("updateCredential => \n\tRealmModel" + rm.toString() + "\n\tstring => " + string + "\n\tCredentialModel => " + cm.toString());
    }

    /**
     * Получает пользоателя. Если пользователь есть в кеше то берет из кеша
     *
     * @param user
     * @return
     */
    public UserAdapter getUserAdapter(UserModel user) {
        log.info("getUserAdapter\n\tuser = " + user);
        UserAdapter adapter = null;
        if (user instanceof CachedUserModel) {
            log.info("User in cache Keycloak");
            adapter = (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
        } else {
            log.info("User not in cache Keycloak");
            adapter = (UserAdapter) user;
        }
        return adapter;
    }

    /**
     *
     * @param realm
     * @param user
     * @param credentialType
     */
    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        log.info("disableCredentialType");
        if (!supportsCredentialType(credentialType)) {
            return;
        }
        getUserAdapter(user).setPassword(null);
    }

    /**
     *
     * @param realm
     * @param user
     * @return
     */
    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        log.info("getDisableableCredentialTypes\n\n\trealm = " + realm.getName() + "\n\tuser = " + user.getUsername());
        if (getUserAdapter(user).getPassword() != null) {
            log.info("\n\tpassword is not null : " + getUserAdapter(user).getPassword());
            Set<String> set = new HashSet<>();
            set.add(CredentialModel.PASSWORD);
            log.info("\n\treturn CredentialModel.PASSWORD = " + CredentialModel.PASSWORD);
            return set;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     *
     * @param realm
     * @param user
     * @param credentialType
     * @return
     */
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        log.info("isConfiguredFor");
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }

    /**
     * Функция проверяет пароль введенный пользователем и пароль сохраненный в
     * базе
     *
     * @param realm
     * @param user
     * @param input
     * @return
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        log.info("isValid\n\trealm = " + realm.getName() + "\n\tuser = " + user.getUsername() + "\n\tinput = " + input.getType());               
        
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }

        UserCredentialModel cred = (UserCredentialModel) input;
        log.info("getHashType");
        String password = getPassword(user);
        // Берем соль
        String salt = getSalt(user);
        boolean flag = false;
        switch ((getHashType(user)).toLowerCase()) {
            case "md5":
                log.info("\n{"
                        + "\n\tcred device= " + cred.getDevice()
                        //+ "\n\tuserpass    = " + cred.getValue()
                        + "\n\tsalt        = " + salt
                        + "\n\tpassword    = " + password
                        + "\n\tuserpass    = " + encodeToHex(md5(cred.getValue() + salt))
                        //+ "\n\tuserpass    = " + hashUtil.md5ToString(cred.getValue() + salt)
                        + "\n}");
                flag = (password != null) && ((password).equals(encodeToHex(md5(cred.getValue() + salt))));
                //flag = (password != null) && ((password).equals(hashUtil.md5ToString(cred.getValue() + salt)));
                //log.info("res = " + flag);
                //return flag;
                break;
            case "sha1":
                log.info("\n{"
                        + "\n\tcred device= " + cred.getDevice()
                        // + "\n\tuserpass    = " + cred.getValue()
                        + "\n\tsalt        = " + salt
                        + "\n\tpassword    = " + password
                        + "\n\tuserpass    = " + encodeToHex(sha1(cred.getValue() + salt))
                        //+ "\n\tuserpass    = " + hashUtil.sha1ToString(cred.getValue() + salt)
                        + "\n}");

                flag = (password != null) && ((password).equals(encodeToHex(sha1(cred.getValue() + salt))));
                //flag = (password != null) && ((password).equals(hashUtil.sha1ToString(cred.getValue() + salt)));

                // log.info("res = " + flag);
                // return flag;
                //(password != null) && ((password).equals(encodeToHex(sha1(cred.getValue() + salt))));
                break;
            default:
                log.info("\n\tcred device= " + cred.getDevice()
                        + "\n\tpassword = " + password
                //     + "\n\tuserpass = " + cred.getValue()
                );
                flag = (password != null) && ((password).equals(cred.getValue()));
                break;
            // log.info("res = " + flag);
            // return flag;
        }
        log.info("res = " + flag);
        //TODO: 08/01/2018  меняем код для подтверждения по СМС
        if (flag) {

//            UsersAuthSmsCode code = new UsersAuthSmsCode();
//            code.setCode(1000);
//
//            ((UserAdapter) user).addUserAuthSmsCode(code);
//            UserCredentialModel input = new UserCredentialModel();
//            input.setType("SECRET_QUESTION");
//            String answer = Math.round(Math.random() * 1000000) + "";
//            input.setValue(answer);
//
//            UserAdapter user = new UserAdapter(session, realm, model, entity, em);
//
//            session.userCredentialManager().updateCredential(realm, (UserModel) user, input);
//            
            String answer = genSalt().substring(0, SECRET_QUESTION_SIZE);
            List<CredentialModel> creds = session.userCredentialManager().getStoredCredentialsByType(realm, user, "SECRET_QUESTION");
            if (creds.isEmpty()) {
                CredentialModel secret = new CredentialModel();
                secret.setType("SECRET_QUESTION");
                secret.setValue(answer);
                secret.setCreatedDate(Time.currentTimeMillis());
                session.userCredentialManager().createCredential(realm, user, secret);
            } else {
                creds.get(0).setValue(answer);
                session.userCredentialManager().updateCredential(realm, user, creds.get(0));
            }
            session.userCache().evict(realm, user);            
        }
        return flag;
    }

    /**
     * Функция получает значение пароля из БД или из Кеша в зависимости есть ли
     * пользователь в кеше
     *
     * @param user
     * @return
     */
    private String getPassword(UserModel user) {
        log.info("getPassword");
        log.info("Class type user = " + user.getClass().getName());
        String password = null;
        if (user instanceof CachedUserModel) {
            log.info("User in cache");
            password = (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
            log.info("password => " + password);
        } else if (user instanceof UserAdapter) {
            log.info("User not in cache");
            password = ((UserAdapter) user).getPassword();
        }
        log.info("password => " + password);
        return password;
    }

    /**
     * Получает значение salt
     *
     * @param user
     * @return
     */
    private String getSalt(UserModel user) {
        log.info("getSalt");
        log.info("Class type user = " + user.getClass().getName());
        String salt = null;
        if (user instanceof CachedUserModel) {
            log.info("User in cache");
            salt = (String) ((CachedUserModel) user).getCachedWith().get(SALT_CACHE_KEY);
            log.info("password => " + salt);
        } else if (user instanceof UserAdapter) {
            log.info("User not in cache");
            salt = ((UserAdapter) user).getSalt();
        }
        log.info("password => " + salt);
        return salt;
    }

    /**
     * Функция возвращает тип hesh пароля
     *
     * @param user
     * @return
     */
    private String getHashType(UserModel user) {
        log.info("getHashType id => " + user.getId());
        String res = null;
        try {
            /*Map<String, List<String>> temp = user.getAttributes();
            temp.forEach((String t, List<String> u) -> {
                log.info("t => " + t + "\t u = " + u);
            });*/
            res = user.getFirstAttribute("hash_type");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.info("getHashType res => " + res);
        return res;
    }

    /**
     *
     * @param realm
     * @return
     */
    @Override
    public int getUsersCount(RealmModel realm) {
        log.info("getUsersCount");
        Object count = em.createNamedQuery("getUserCount").getSingleResult();
        return ((Number) count).intValue();
    }

    /**
     *
     * @param realm
     * @return
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        log.info("getUsers");
        return getUsers(realm, -1, -1);
    }

    /**
     *
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        log.info("getUsers_1\n\n\trealm = " + realm.getName() + "\n\tfirstResult = " + firstResult + "\n\tmaxResults = " + maxResults);
        TypedQuery<UserEntity> query = em.createNamedQuery("getAllUsers", UserEntity.class);
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) {
            users.add(new UserAdapter(session, realm, model, entity, em));
        }
        return users;
    }

    /**
     *
     * @param search
     * @param realm
     * @return
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        log.info("searchForUser");
        return searchForUser(search, realm, -1, -1);
    }

    /**
     *
     * @param search
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        log.info("searchForUser");
        TypedQuery<UserEntity> query = em.createNamedQuery("searchForUser", UserEntity.class);
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) {
            users.add(new UserAdapter(session, realm, model, entity, em));
        }
        return users;
    }

    /**
     *
     * @param params
     * @param realm
     * @return
     */
    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        log.info("searchForUser_1");
        List<UserEntity> res;
        List<UserModel> users = new LinkedList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> criteriaQuery = cb.createQuery(UserEntity.class);
            Root<UserEntity> userEntityRoot = criteriaQuery.from(UserEntity.class);
            Predicate criteriaWhere = cb.conjunction();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                log.info("key = " + key + "  value = " + value);
                if (key.equalsIgnoreCase("username")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.USERNAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("email")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.EMAIL), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("firstName")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.FIRST_NAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("lastName")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.LAST_NAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }
            }

            criteriaQuery.where(criteriaWhere);

            res = em.createQuery(criteriaQuery).getResultList();
            log.info("res = " + res.size());
            res.forEach((item) -> {
                users.add(new UserAdapter(session, realm, model, item, em));
            });

        } catch (Exception e) {
            log.log(Logger.Level.ERROR, e);
        }

        return users;
        //return Collections.EMPTY_LIST;
    }

    /**
     * Выполняется при вызове RAST API функции поиска пользователей по критериям
     *
     * @param params
     * @param realm
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        log.info("searchForUser_2 params => " + params.size() + " realm => " + realm + " firstResalt => " + firstResult + "maxResult => " + maxResults);

        List<UserEntity> res;
        List<UserModel> users = new LinkedList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<UserEntity> criteriaQuery = cb.createQuery(UserEntity.class);
            Root<UserEntity> userEntityRoot = criteriaQuery.from(UserEntity.class);
            Predicate criteriaWhere = cb.conjunction();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                log.info("key = " + key + "  value = " + value);
                if (key.equalsIgnoreCase("username")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.USERNAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("email")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.EMAIL), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("firstName")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.FIRST_NAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }

                if (key.equalsIgnoreCase("lastName")) {
                    Predicate p = cb.equal(userEntityRoot.get(UserModel.LAST_NAME), value);
                    criteriaWhere = cb.and(criteriaWhere, p);
                }
            }

            criteriaQuery.where(criteriaWhere);

            res = em.createQuery(criteriaQuery).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
            log.info("res = " + res.size());
            res.forEach((item) -> {
                users.add(new UserAdapter(session, realm, model, item, em));
            });

        } catch (Exception e) {
            log.log(Logger.Level.ERROR, e);
        }

        return users;
        //Collections.EMPTY_LIST;
    }

    /**
     *
     * @param attrName
     * @param attrValue
     * @param realm
     * @return
     */
    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        log.info("searchForUserByUserAttribute attrName => " + attrName + " attrValue => " + attrValue + " realm => " + realm);
        return Collections.EMPTY_LIST;
    }

    /**
     *
     * @param realm
     * @param group
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        log.info("getGroupMembers realm => " + realm + " group => " + group + " firstResult => " + firstResult + " maxResults => " + maxResults);
        return Collections.EMPTY_LIST;
    }

    /**
     *
     * @param realm
     * @param group
     * @return
     */
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        log.info("getGroupMembers_1 realm => " + realm + " group => " + group);
        return Collections.EMPTY_LIST;
    }

    /**
     *
     * @param rm
     * @param i
     * @param i1
     * @return
     */
    @Override
    public List<String> getStoredUsers(RealmModel rm, int i, int i1) {
        log.info("getStoredUsers rm => " + rm + " i => " + i + " i1 => " + i1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @return
     */
    @Override
    public int getStoredUsersCount(RealmModel rm) {
        log.info("getStoredUsersCount rm => " + rm);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param cm
     */
    @Override
    public void preRemove(RealmModel rm, ClientModel cm) {
        log.info("preRemove_1 rm => " + rm + " cm => " + cm);
    }

    /**
     *
     * @param pmm
     */
    @Override
    public void preRemove(ProtocolMapperModel pmm) {
        log.info("preRemove_2 pmm => " + pmm);
    }

    /**
     *
     * @param rm
     * @param um
     */
    @Override
    public void preRemove(RealmModel rm, UserModel um) {
        log.info("preRemove_3 rm => " + rm + " um => " + um);
    }

    /**
     *
     * @param rm
     * @param cm
     */
    @Override
    public void preRemove(RealmModel rm, ComponentModel cm) {
        log.info("preRemove_4 rm=> " + rm + " cm => " + cm);
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @param string2
     */
    @Override
    public void setSingleAttribute(RealmModel rm, String string, String string1, String string2) {
        log.info("setSingleAttribute rm => " + rm + " string => " + string + " string1 => " + string1 + " string2 => " + string2);
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @param list
     */
    @Override
    public void setAttribute(RealmModel rm, String string, String string1, List<String> list) {
        log.info("setAttribute_1 rm => " + rm + " string => " + string + " string1 => " + string1 + " list => " + list.size());
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     */
    @Override
    public void removeAttribute(RealmModel rm, String string, String string1) {
        log.info("setAttribute_2 rm => " + rm + " string => " + string + " string1 => " + string1);
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public MultivaluedHashMap<String, String> getAttributes(RealmModel rm, String string) {
        log.info("getAttributes rm => " + rm + " string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public List<String> getUsersByUserAttribute(RealmModel rm, String string, String string1) {
        log.info("getUsersByUserAttribute rm => " + rm + " string => " + string + " string1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param fim
     * @param rm
     * @return
     */
    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel fim, RealmModel rm) {
        log.info("getUserByFederatedIdentity => " + fim.getUserId() + " : " + fim.getUserName());
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return "";
    }

    /**
     *
     * @param rm
     * @param string
     * @param fim
     */
    @Override
    public void addFederatedIdentity(RealmModel rm, String string, FederatedIdentityModel fim) {
        log.info("addFederatedIdentity rm => " + rm + " string => " + string + "fim => " + fim);
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public boolean removeFederatedIdentity(RealmModel rm, String string, String string1) {
        log.info("removeFederatedIdentity rm => " + rm + " string => " + string + " string1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param fim
     */
    @Override
    public void updateFederatedIdentity(RealmModel rm, String string, FederatedIdentityModel fim) {
        log.info("updateFederatedIdentity rm => " + rm + " string => " + string + " fim => " + fim);
    }

    /**
     *
     * @param string
     * @param rm
     * @return
     */
    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(String string, RealmModel rm) {
        log.info("getFederatedIdentities string => " + string + " rm => " + rm);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param string
     * @param string1
     * @param rm
     * @return
     */
    @Override
    public FederatedIdentityModel getFederatedIdentity(String string, String string1, RealmModel rm) {
        log.info("getFederatedIdentity string => " + string + " string1 => " + string1 + " rm => " + rm);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param ucm
     */
    @Override
    public void addConsent(RealmModel rm, String string, UserConsentModel ucm) {
        log.info("addConsent rm => " + rm + " string => " + string + " ucm => " + ucm);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public UserConsentModel getConsentByClient(RealmModel rm, String string, String string1) {
        log.info("getConsentByClient rm => " + rm + " string => " + string + " string1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public List<UserConsentModel> getConsents(RealmModel rm, String string) {
        log.info("getConsents => " + rm + " string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param ucm
     */
    @Override
    public void updateConsent(RealmModel rm, String string, UserConsentModel ucm) {
        log.info("updateConsent");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public boolean revokeConsentForClient(RealmModel rm, String string, String string1) {
        log.info("revokeConsentForClient");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public Set<GroupModel> getGroups(RealmModel rm, String string) {
        log.info("getGroups rm => " + rm + " string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param gm
     */
    @Override
    public void joinGroup(RealmModel rm, String string, GroupModel gm) {
        log.info("joinGroup => " + string + " name => " + gm.getName());
    }

    /**
     *
     * @param rm
     * @param string
     * @param gm
     */
    @Override
    public void leaveGroup(RealmModel rm, String string, GroupModel gm) {
        log.info("leaveGroup rm => " + rm + " string => " + string + " gm => " + gm);
    }

    /**
     *
     * @param rm
     * @param gm
     * @param i
     * @param i1
     * @return
     */
    @Override
    public List<String> getMembership(RealmModel rm, GroupModel gm, int i, int i1) {
        log.info("getMembership rm => " + rm + " gm => " + gm + " i => " + i + " i1 => " + i1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public Set<String> getRequiredActions(RealmModel rm, String string) {
        log.info("getRequiredActions rm => " + rm + " string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     */
    // TODO : requaredAction
//    @Override
//    public void addRequiredAction(RealmModel rm, String string, String string1) {
//        log.info("addRequiredAction => " + rm + " string => " + string + " string1 => " + string1);
//    }
    /**
     *
     * @param rm
     * @param string
     * @param string1
     */
//    @Override
//    public void removeRequiredAction(RealmModel rm, String string, String string1) {
//        log.info("removeRequiredAction rm => " + rm + " string => " + string + " string1 => " + string1);
//    }
    /**
     *
     * @param rm
     * @param string
     * @param rm1
     */
    @Override
    public void grantRole(RealmModel rm, String string, RoleModel rm1) {
        log.info("grantRole rm => " + rm + " rm1 => " + rm1 + " string => " + string);
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public Set<RoleModel> getRoleMappings(RealmModel rm, String string) {
        log.info("getRoleMappings rm => " + rm + " string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param rm1
     */
    @Override
    public void deleteRoleMapping(RealmModel rm, String string, RoleModel rm1) {
        log.info("deleteRoleMapping string => " + string + " rm1 => " + rm + " rm1 => " + rm1);
    }

    /**
     *
     * @param rm
     * @param string
     * @param cm
     * @return
     */
    @Override
    public CredentialModel createCredential(RealmModel rm, String string, CredentialModel cm) {
        log.info("createCredential cm => " + cm);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public boolean removeStoredCredential(RealmModel rm, String string, String string1) {
        log.info("removeStoredCredential string => " + string + " String1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public CredentialModel getStoredCredentialById(RealmModel rm, String string, String string1) {
        log.info("getStoredCredentialById string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @return
     */
    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel rm, String string) {
        log.info("getStoredCredentials string => " + string);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @return
     */
    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel rm, String string, String string1) {
        log.info("getStoredCredentialsByType string => " + string + " String1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param rm
     * @param string
     * @param string1
     * @param string2
     * @return
     */
    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel rm, String string, String string1, String string2) {
        log.info("getStoredCredentialByNameAndType => " + string + " String1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addRequiredAction(RealmModel rm, String string, String string1) {
        log.debug("addRequiredAction => " + rm + " string => " + string + " string1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeRequiredAction(RealmModel rm, String string, String string1) {
        log.debug("removeRequiredAction => " + rm + " string => " + string + " string1 => " + string1);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
