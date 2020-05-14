package main

import (
    "fmt"
    "log"
    "net/http"
    "io/ioutil"
    "encoding/json"
    "os"
    "strings"
)

type Target struct {
  Targets []string
  Labels Label
}

type Label struct {
  Job string
}

func (t Target) toString() string {
  result := "{\"targets\":["
  for i, t := range t.Targets {
    if i>0 {result = result + ","}
    result = result + "\"" + t + "\""
  }
  result = result + "],\"labels\":{\"job\":\"" + t.Labels.Job + "\"}}"
  return result
}

func (t Target) isValid() bool {
  return true
}

func (t Target) remove(conf string) (removed bool, e error) {
  targetAsString := t.toString()
  b, err := ioutil.ReadFile(conf)
  if err != nil {
    return false, err
  }
  content := string(b)
  if strings.Contains(content, targetAsString) {
    content = strings.ReplaceAll(content, t.toString(), "")
    f, err := os.OpenFile(conf, os.O_WRONLY, 0644)
    if err != nil {
      return false, err
    }
    defer f.Close()
    if _, err := f.WriteString(content); err != nil {
      return false, err
    } else {
      return true, err
    }
  } else {
    return false, nil
  }

}

func (t Target) add(conf string) (added bool, e error) {
  targetAsString := t.toString()
  b, err := ioutil.ReadFile(conf)
  if err != nil {
    return false, err
  }

  content := string(b)
  if strings.Contains(content, targetAsString) {
    return false, nil
  }

  f, err := os.OpenFile(conf, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
  if err != nil {
    return false, err
  }

  defer f.Close()
  if _, err := f.WriteString(targetAsString+"\n"); err != nil {
    return false, err
  } else {
    return true, err
  }

}

func parse(r *http.Request) (t Target, e error) {
  var target Target
  b, err := ioutil.ReadAll(r.Body)
  if err != nil {return target, err}
  json.Unmarshal(b, &target)
  return target, nil
}

func main() {
  http.HandleFunc("/targets", func(w http.ResponseWriter, r *http.Request) {
    if r.Method == http.MethodPost || r.Method == http.MethodPut {
      t, parseErr := parse(r)
      if parseErr == nil && t.isValid() {
        added, addErr := t.add("targets.json")
        if addErr == nil && added {
          fmt.Fprintf(w, "Added target %s", t.toString())
        } else if addErr == nil && !added {
          fmt.Fprintf(w, "Target %s already present", t.toString())
        } else {
          fmt.Fprintf(w, "Error while adding target %s: ", t.toString())
        }
      }
      return
    }
    if r.Method == http.MethodDelete {
      t, parseErr := parse(r)
      if parseErr == nil && t.isValid() {
        removed, rmErr := t.remove("targets.json")
        if rmErr == nil && removed {
          fmt.Fprintf(w, "Removed target %s", t.toString())
        } else if rmErr == nil && !removed {
          fmt.Fprintf(w, "Target %s was not present", t.toString())
        } else {
          fmt.Fprintf(w, "Error while removing target %s: ", t.toString())
        }
      }
      return
    }
    fmt.Fprintf(w, "Method %s not allowed!", r.Method)
    return
  })

  log.Fatal(http.ListenAndServe(":8080", nil))
}
