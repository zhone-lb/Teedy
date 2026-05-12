package com.sismics.docs.core.dao;
import com.sismics.docs.BaseTransactionalTest;
import com.sismics.docs.core.dao.criteria.UserCriteria;
import com.sismics.docs.core.dao.dto.UserDto;
import com.sismics.docs.core.model.jpa.Group;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserGroup;
import com.sismics.docs.core.util.jpa.SortCriteria;
import org.junit.Assert;
import org.junit.Test;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests {@link UserDao#findByCriteria(com.sismics.docs.core.dao.criteria.UserCriteria, SortCriteria)}.
 */
public class TestUserDaoFindByCriteria extends BaseTransactionalTest {
    @Test
    public void findByCriteria_emptyCriteria_listsNonDeletedUsers() throws Exception {
        UserDao userDao = new UserDao();
        User u = createUser("fbc_empty_a");
        createUser("fbc_empty_b");
        List<UserDto> list = userDao.findByCriteria(new UserCriteria(), null);
        List<String> ids = list.stream().map(UserDto::getId).collect(Collectors.toList());
        Assert.assertTrue(ids.contains(u.getId()));
        Assert.assertTrue(list.size() >= 2);
    }
    
    @Test
    public void findByCriteria_search_matchesUsernameSubstring() throws Exception {
        UserDao userDao = new UserDao();
        User match = createUser("fbc_SearchMatch_z");
        createUser("fbc_other");
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setSearch("searchmatch"), null);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(match.getId(), list.get(0).getId());
    }
    
    @Test
    public void findByCriteria_search_noResults() throws Exception {
        UserDao userDao = new UserDao();
        createUser("fbc_nosearch");
        List<UserDto> list = userDao.findByCriteria(
                new UserCriteria().setSearch("unlikely_xyz_no_match_999"), null);
        Assert.assertTrue(list.isEmpty());
    }
 @Test
    public void findByCriteria_userId() throws Exception {
        UserDao userDao = new UserDao();
        User u = createUser("fbc_byid");
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setUserId(u.getId()), null);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(u.getId(), list.get(0).getId());
        Assert.assertEquals("fbc_byid", list.get(0).getUsername());
    }

    
    @Test
    public void findByCriteria_userName() throws Exception {
        UserDao userDao = new UserDao();
        User u = createUser("fbc_byname");
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setUserName("fbc_byname"), null);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(u.getId(), list.get(0).getId());
    }

    
    @Test
    public void findByCriteria_groupId_onlyMembers() throws Exception {
        UserDao userDao = new UserDao();
        GroupDao groupDao = new GroupDao();
        User inGroup = createUser("fbc_group_in");
        User outGroup = createUser("fbc_group_out");
        Group group = new Group().setName("fbc_group");
        groupDao.create(group, inGroup.getId());
        UserGroup link = new UserGroup();
        link.setUserId(inGroup.getId());
        link.setGroupId(group.getId());
        groupDao.addMember(link);

        
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setGroupId(group.getId()), null);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(inGroup.getId(), list.get(0).getId());
        List<UserDto> allInGroup = userDao.findByCriteria(
                new UserCriteria().setGroupId(group.getId()).setUserName(outGroup.getUsername()), null);
        Assert.assertTrue(allInGroup.isEmpty());
    }


    @Test
    public void findByCriteria_combinedSearchAndUserId() throws Exception {
        UserDao userDao = new UserDao();
        User u = createUser("fbc_combo_user");
        List<UserDto> list = userDao.findByCriteria(
                new UserCriteria().setUserId(u.getId()).setSearch("combo"), null);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(u.getId(), list.get(0).getId());
    }


    @Test
    public void findByCriteria_sortAscendingByUsername() throws Exception {
        UserDao userDao = new UserDao();
        createUser("fbc_sort_b");
        createUser("fbc_sort_a");
        List<UserDto> list = userDao.findByCriteria(
                new UserCriteria().setSearch("fbc_sort_"),
                new SortCriteria(1, true));
        List<String> names = list.stream().map(UserDto::getUsername).collect(Collectors.toList());
        Assert.assertTrue(names.indexOf("fbc_sort_a") < names.indexOf("fbc_sort_b"));
    }
    
    @Test
    public void findByCriteria_sortDescendingByUsername() throws Exception {
        UserDao userDao = new UserDao();
        createUser("fbc_sdesc_b");
        createUser("fbc_sdesc_a");
        List<UserDto> list = userDao.findByCriteria(
                new UserCriteria().setSearch("fbc_sdesc_"),
                new SortCriteria(1, false));
        List<String> names = list.stream().map(UserDto::getUsername).collect(Collectors.toList());
        Assert.assertTrue(names.indexOf("fbc_sdesc_b") < names.indexOf("fbc_sdesc_a"));
    }


    @Test
    public void findByCriteria_disableTimestamp_nullWhenActive() throws Exception {
        UserDao userDao = new UserDao();
        createUser("fbc_active");
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setUserName("fbc_active"), null);
        Assert.assertEquals(1, list.size());
        Assert.assertNull(list.get(0).getDisableTimestamp());
    }


    @Test
    public void findByCriteria_disableTimestamp_whenDisabled() throws Exception {
        UserDao userDao = new UserDao();
        User u = createUser("fbc_disabled");
        Date disabledAt = new Date();
        User patch = new User();
        patch.setId(u.getId());
        patch.setEmail("toto@docs.com");
        patch.setStorageQuota(100_000L);
        patch.setStorageCurrent(0L);
        patch.setTotpKey(null);
        patch.setDisableDate(disabledAt);
        userDao.update(patch, u.getId());
        
        List<UserDto> list = userDao.findByCriteria(new UserCriteria().setUserId(u.getId()), null);
        Assert.assertEquals(1, list.size());
        Assert.assertNotNull(list.get(0).getDisableTimestamp());
        Assert.assertEquals(disabledAt.getTime(), list.get(0).getDisableTimestamp().longValue());
    }
}
