package uff.redes.iot.dht;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uff.redes.iot.tcp.TCPServer;
//
//@Controller
//@RequiredArgsConstructor
//public class DHTPageController {
//
//    private final DHTService service;
//
//    @GetMapping("/dht") // acessa diretamente http://IP:8080/dht
//    public String paginaDHT(Model model) {
//        DHTResponse ultimo = service.buscarUltimo();
//        model.addAttribute("dht", ultimo);
//        return "dht"; // templates/dht.html
//    }
//}


@Controller
@RequiredArgsConstructor
public class DHTPageController {

    private final TCPServer tcpServer;

    @GetMapping("/dht")
    public String paginaDHT(Model model) {
        model.addAttribute("dht", tcpServer.getLastData());
        return "dht";
    }

    @GetMapping("/api/dht/ultimo")
    @ResponseBody
    public DHTResponse ultimo() {
        return tcpServer.getLastData();
    }
}
