function global_func()
	local function local_func()
		print("call local func")
	end
	print("call global_func")
	local_func()
end

global_func()