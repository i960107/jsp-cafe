package com.codesquad.cafe.servlet;

import static com.codesquad.cafe.util.SessionUtil.getUserPrincipal;

import com.codesquad.cafe.db.UserRepository;
import com.codesquad.cafe.db.entity.User;
import com.codesquad.cafe.model.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userRepository = (UserRepository) getServletContext().getAttribute("userRepository");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long id = parsePathVariable(req.getPathInfo());
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            resp.sendError(404);
            return;
        }
        req.setAttribute("user", user.get());

        req.getRequestDispatcher("/WEB-INF/views/user_profile.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getParameter("_method");
        if (method != null && method.toLowerCase(Locale.ROOT).equals("delete")) {
            doDelete(req, resp);
            return;
        }
        resp.sendError(405);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        UserPrincipal userPrincipal = getUserPrincipal(req);
        if (userPrincipal == null) {
            resp.sendError(401);
            return;
        }

        Optional<User> user = userRepository.findById(userPrincipal.getId());

        // 존재하지 않는 유저이거나 이미 탈퇴한 유저의 경우
        if (user.isEmpty() || user.get().isDeleted()) {
            resp.sendError(400);
            return;
        }

        // 유저 soft delete
        User originalUser = user.get();
        originalUser.delete();
        userRepository.save(originalUser);

        //session invalidate
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        resp.sendRedirect("/users/list");
    }

    private Long parsePathVariable(String pathInfo) {
        String[] paths = pathInfo.substring(1).split("/");
        return Long.parseLong(paths[0]);
    }

}
