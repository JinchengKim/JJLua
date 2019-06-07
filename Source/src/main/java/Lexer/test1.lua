function fact (n)
    if n == 0 then
      return 1
    else
      return fact(n-1) * n
    end
  end

a = 4
print("Factorial of 4 = ")
print(fact(a))