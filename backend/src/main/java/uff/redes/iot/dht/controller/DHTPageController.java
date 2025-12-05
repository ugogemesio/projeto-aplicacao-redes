package uff.redes.iot.dht.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uff.redes.iot.dht.model.DHTResponse;
import uff.redes.iot.dht.service.DHTService;
import uff.redes.iot.tcp.TCPServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
public class DHTPageController {
private final DHTService service;
    private final TCPServer tcpServer;

    @GetMapping("/dht")
    public String paginaDHT(Model model) {
        model.addAttribute("dht", tcpServer.getLastData());
        return "dht";
    }

}
