package com.max.controller;

import com.max.domain.Message;
import com.max.domain.User;
import com.max.repository.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private MessageRepo messageRepo;

    //Это значит, что мы идем в properties -> там спринг находит переменную upload.file и подставляет ее сюда
    @Value("${upload.path}")
    private String uploadPath;

    //Mapping - для проверки (так чисто для красоты)
    @GetMapping("/")
    public String greeting( Model model) {

        return "greeting";
    }

    //Mapping главной страницы
    @GetMapping("/main")
    public String main(@RequestParam(required = false, defaultValue = "") String filter, Model model) {
        Iterable<Message> messages = messageRepo.findAll();

        if(filter != null && !filter.isEmpty()) {
            messages = messageRepo.findByTag(filter);
        }
        else {
            messages = messageRepo.findAll();
        }
        model.addAttribute("filter", filter);
        model.addAttribute("messages", messages);
        return "main";
    }

    //Mapping для добавления message
    @PostMapping("/main")
    public String add(
            @AuthenticationPrincipal User user,
            @Valid Message message,
            BindingResult bindingResult, //нужно для проверки ошибки при валидации (ВСЕГДА ДОЛЖЕН ИДТИ ПЕРЕД MODEL)
            Model model,
            @RequestParam("file") MultipartFile file) throws IOException {

        //если у нас есть ошибки то сразу будем их обрабатывать
        if(bindingResult.hasErrors()) {// создадим мапу в которую будем записывать наши ошибки
            Map<String, String> errorMap = ControllerUtils.getErrors(bindingResult);
            model.mergeAttributes(errorMap);
            model.addAttribute("message", message);
        } else {
            message.setAuthor(user);

            saveFile(message, file);

            //если валидация прошла успешно, то месседж (в нем будут наши ошибки) нужно очистить
            model.addAttribute("message", null);

            messageRepo.save(message);
        }
        Iterable<Message> messages = messageRepo.findAll();

        model.addAttribute("messages", messages);

        return "main";
    }

    private void saveFile(@Valid Message message, @RequestParam("file") MultipartFile file) throws IOException {
        if (file != null && !file.getOriginalFilename().isEmpty()) {

            File uploadDir = new File(uploadPath);
            //если дериктория  не существует - создадим ее
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }
            //обезопасим себя от коллизий
            String uuidFile = UUID.randomUUID().toString();

            //это будет имя файла, котое мы будем ложить в месседж
            String resultFileName = uuidFile + "." + file.getOriginalFilename();

            //теперь этот файл нужно загрузить
            file.transferTo(new File(uploadPath + "/" + resultFileName));
            message.setFilename(resultFileName);
        }
    }

    //отображение сообщений юзера
    @GetMapping("/user-messages/{user}")
    public String userMessages(
            @AuthenticationPrincipal User currentUser,  //берем текущего пользователя у сесси
            @PathVariable User user,
            Model model,
            @RequestParam(required = false) Message message
    ) {
        Set<Message> messages = user.getMessages();

        model.addAttribute("userChannel", user);
        model.addAttribute("subscriptionsCount", user.getSubscriptions().size());
        model.addAttribute("subscribersCount", user.getSubscribers().size());

        model.addAttribute("isSubscriber", user.getSubscribers().contains(currentUser));//является ли текущий юзер подписчиком того юзера к которому мы пришли
        model.addAttribute("messages", messages);
        model.addAttribute("message", message);

        model.addAttribute("isCurrentUser", currentUser.equals(user)); //совпадают ли текущий пользователь, полученный из БД с юзером из сессии

        return "userMessages";
    }

    //для редактирования выбранного сообщения
    @PostMapping("/user-messages/{user}")
    public String updateMessage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long user,
            @RequestParam("id") Message message,
            @RequestParam("text") String text,
            @RequestParam("tag") String tag,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        //юзер может только свои сообщения,
        if (message.getAuthor().equals(currentUser)) {
            if (!StringUtils.isEmpty(text)) { //меняем текст
                message.setText(text);
            }

            if (!StringUtils.isEmpty(tag)) {  //меняем тег
                message.setTag(tag);
            }

            saveFile(message, file);  //меняем файл

            messageRepo.save(message);
        }

        return "redirect:/user-messages/" + user;
    }
}
