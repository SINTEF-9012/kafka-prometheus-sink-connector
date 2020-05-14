package main

import (
    "fmt"
    "log"
    "net/http"
    "io/ioutil"
    "strings"
)

func load(conf string) []string {
  var result []string
  b, err := ioutil.ReadFile(conf)
  if err == nil {
    content := string(b)
    for _, s := range strings.Split(content, "\n\n") {
        result = append(result, s)
    }
  }
  return result
}

func save(conf string, targets []string) error {
  content := strings.Join(targets, "\n\n")
  return ioutil.WriteFile(conf, []byte(content), 0644)
}

func add(conf string, target string) bool {
  targets := load(conf)
  index := -1
  for i, current := range targets {
    if current == target {
      index = i
      break
    }
  }
  if index == -1 {
    targets = append(targets, target)
    err := save(conf, targets)
    if err == nil {
      return true
    }
  }
  return false
}

func remove(conf string, target string) bool {
  targets := load(conf)
  index := -1
  for i, current := range targets {
    if current == target {
      index = i
      break
    }
  }
  if index != -1 {
    targets[index] = targets[len(targets)-1]
    //targets[len(targets)-1] = nil
    targets = targets[:len(targets)-1]
    err := save(conf, targets)
    if err == nil {
      return true
    }
  }
  return false
}

func get(conf string) string {
  b, err := ioutil.ReadFile(conf)
  if err != nil {
    return ""
  }
  content := string(b)
  return content
}

func parse(r *http.Request) (address string, job string, e error) {
  b, err := ioutil.ReadAll(r.Body)
  return strings.Split(string(b), " ")[0], strings.Split(string(b), " ")[1], err
}

func main() {
  http.HandleFunc("/targets", func(w http.ResponseWriter, r *http.Request) {
    if r.Method == http.MethodPost || r.Method == http.MethodPut {
      address, job, parseErr := parse(r)
      if parseErr == nil {//FIXME: sanity check on t
        t := "- targets: ['" + address + "']\n  labels:\n    job: '" + job + "'"
        added := add("/etc/prometheus/targets.yml", t)
        if added {
          fmt.Fprintf(w, "Added target %s", t)
        } else {
          fmt.Fprintf(w, "Target %s already present", t)
        }
      }
      return
    } else if r.Method == http.MethodDelete {
      address, job, parseErr := parse(r)
      if parseErr == nil {//FIXME: sanity check on t
        t := "- targets: ['" + address + "']\n  labels:\n    job: '" + job + "'"
        removed := remove("/etc/prometheus/targets.yml", t)
        if removed {
          fmt.Fprintf(w, "Removed target %s", t)
        } else {
          fmt.Fprintf(w, "Target %s was not present", t)
        }
      }
      return
    } else {
      fmt.Fprintf(w, "%s", get("/etc/prometheus/targets.yml"))
      return
    }
  })

  log.Fatal(http.ListenAndServe(":8080", nil))
}
