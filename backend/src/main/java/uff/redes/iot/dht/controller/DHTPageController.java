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

//    @GetMapping("/dht")
//    public String paginaDHT(Model model) {
//        model.addAttribute("dht", tcpServer.getLastData());
//        return "dht";
//    }




    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<DHTResponse> dados = service.listarTodos();
        double maxT = dados.stream().mapToDouble(DHTResponse::temperatura).max().orElse(0);
        double minT = dados.stream().mapToDouble(DHTResponse::temperatura).min().orElse(0);
        double avgT = dados.stream().mapToDouble(DHTResponse::temperatura).average().orElse(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("maxTemp", maxT);
        stats.put("minTemp", minT);
        stats.put("avgTemp", avgT);
        return stats;
    }

}
