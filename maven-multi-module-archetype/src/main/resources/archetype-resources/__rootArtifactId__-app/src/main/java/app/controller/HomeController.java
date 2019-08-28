package ${package}.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/home")
public class HomeController {

    @GetMapping("/index")
    public Map<String, Object> home() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("code", 0);
        map.put("message", "Ok");

        return map;
    }
}
