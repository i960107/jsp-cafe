package com.codesquad.cafe.servlet;

import static com.codesquad.cafe.util.PathVariableUtil.parsePathVariable;

import com.codesquad.cafe.db.UserRepository;
import com.codesquad.cafe.db.entity.User;
import com.codesquad.cafe.exception.ResourceNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsersServlet extends HttpServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserRepository userRepository;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userRepository = (UserRepository) getServletContext().getAttribute("userRepository");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            doGetList(req, resp);
            return;
        }
        Long id = parsePathVariable(req.getPathInfo());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException());

        req.setAttribute("user", user);

        req.getRequestDispatcher("/WEB-INF/views/user_profile.jsp").forward(req, resp);
    }

    protected void doGetList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<User> users = userRepository.findAll();
        req.setAttribute("users", users);
        req.setAttribute("total", users.size());
        req.getRequestDispatcher("/WEB-INF/views/users.jsp").forward(req, resp);
    }

}

