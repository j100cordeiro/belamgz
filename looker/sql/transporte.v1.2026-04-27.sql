select
	REL.*,
	ITE.QTDNEG,
	PAR.CGC_CPF CPF,
	NVL(TRAT.DESCRICAO,
	' ') Tratativa,
	NVL(HIST.OBSERVACAO,
	' ') Observacao,
	HIST.DHINC ULTINTERACAO,
	USU.NOMEUSU ULTUSUINTER,
	USU.CODUSU CODUSUINTER,
	case
		when HIST.SITUACAO is not null then SANKHYA.OPTION_LABEL('AD_BHZHIST',
		'SITUACAO',
		HIST.SITUACAO)
		when DTENTREGANF is not null then 'Entregue'
		else 'Em Rota'
	end Situacao,
	case
		when DIFERENCA_EM_DIAS > 0
		and STATUS != 'Devolucao' then 'RED'
		else case
			when DIF_DATAS < 0
			and STATUS != 'Devolucao' then '#B8860B'
			else null
		end
	end FGCOLOR,
	case
		when case
			when HIST.SITUACAO is not null then SANKHYA.OPTION_LABEL('AD_BHZHIST',
			'SITUACAO',
			HIST.SITUACAO)
			when DTENTREGANF is not null then 'Entregue'
			else 'Em Rota'
		end in ('Em Rota', 'Entregue', 'Devolvido', 'Indenização Paga') then 'DENTRO DO PRAZO'
		when ROUND((SYSDATE - HIST.DHINC) * 24,
		0) > TRAT.SLA then 'FORA DO PRAZO'
		else 'DENTRO DO PRAZO'
	end STATUSSLA,
	case
		when REL.CODPARCTRANSP in (220344, 224984) then 'Meli'
		when REL.CODPARCTRANSP in (285038) then 'Amazon'
		when REL.CODPARCTRANSP in (985588, 984687) then 'Shopee'
		else 'Proprio'
	end TIPENV,
	case
		when REL.CODPARCTRANSP in (220344, 224984) then 'M'
		when REL.CODPARCTRANSP in (285038) then 'A'
		when REL.CODPARCTRANSP in (985588, 984687) then 'S'
		else 'P'
	end TIPENVFILTRO,
	OTRAT.DESCRICAO ORIGEMTRAT,
	REL.CUSTOFRETE-REL.VLRFRETE RESULTFRETE
from
	BHZ_RELTRANSP REL
left join AD_BHZHIST HIST on
	HIST.NUNOTA = REL.NUNOTA
	and (REL.SEQUENCIA = HIST.SEQUENCIA
		or HIST.SEQUENCIA is null)
	and HIST.SEQ = (
	select
		MAX(SEQ)
	from
		AD_BHZHIST
	where
		NUNOTA = REL.NUNOTA
		and (REL.SEQUENCIA = SEQUENCIA
			or SEQUENCIA is null))
left join TSIUSU USU on
	USU.CODUSU = HIST.CODUSU
left join AD_BHZOTRAT OTRAT on
	OTRAT.CODOTRAT = HIST.CODOTRAT
left join AD_BHZTRAT TRAT on
	TRAT.CODTRAT = HIST.CODTRAT
inner join TGFPAR PAR on
	PAR.CODPARC = REL.CODPARC
inner join TGFITE ITE on
	ITE.NUNOTA = REL.NUNOTA
	and ITE.SEQUENCIA = REL.SEQUENCIA
where
	((DIFERENCA_EM_DIAS > 0
		and :ATRASADO = 'S')
	or :ATRASADO = 'N')
	and REL.DTNEG between :DT.INI and :DT.FIN
	and REL.CODTIPOPER in :CODTIPOPER
	and REL.CODPARCTRANSP not in ( 401961, 271827, 18789, 422862, 16, 0)
	and REL.CODPARCTRANSP in (
	select
		CODPARC
	from
		TGFPAR
	where
		AD_CODTRANSPINT in :CODPARCTRANSP)
	and (REL.NUMNOTA = :NUMNOTA
		or :NUMNOTA is null)
	and (REL.CODPARC = :CODPARC
		or :CODPARC is null)
	and (DTENTREGANF is null
		or :ENTREGA = 'N')
	and REL.CODEMP in :CODEMP
	and (NVL(USU.CODUSU,
	0) = :CODUSU
		or :CODUSU is null)
	and (REL.STATUS = case
		when :STATUS = 0 then 'Devolvido'
		when :STATUS = 1 then 'Aprovada'
		else ' '
	end
		or :STATUS = 99)
	and (HIST.SITUACAO = case
		when :SITUACAO = 0 then 'Em Rota'
		when :SITUACAO = 1 then 'Em Tratativa'
		when :SITUACAO = 2 then 'Bloqueio'
		when :SITUACAO = 3 then 'Extravio'
		when :SITUACAO = 4 then 'Entregue'
		when :SITUACAO = 5 then 'Devolvido'
		when :SITUACAO = 6 then 'Devolução'
		when :SITUACAO = 7 then 'Indenização'
		when :SITUACAO = 8 then 'Indenização Paga'
		when :SITUACAO = 9 then 'Coleta'
		when :SITUACAO = 10 then 'Emitir NFe Devolução'
		when :SITUACAO = 11 then 'NF-e sem CT-e'
		when :SITUACAO = 12 then 'Finalizada'
		when :SITUACAO = 13 then 'Ostensiva'
		when :SITUACAO = 14 then 'Negativado'
		when :SITUACAO = 15 then 'Fraude'
		when :SITUACAO = 16 then 'Finalizada com Ressalva'
		else ' '
	end
		or :SITUACAO = 99 )
	and (REL.SKU = :SKU
		or :SKU is null)
	and (PAR.CGC_CPF = :CPF
		or :CPF is null)
	and case
		when REL.CODPARCTRANSP in (220344, 224984) then 'M'
		when REL.CODPARCTRANSP in (285038) then 'A'
		when REL.CODPARCTRANSP in (985588, 984687) then 'S'
		else 'P'
	end in :TIPOENV
	and (REL.BH_CODEMKT like '%' ||:CODMKT || '%'
		or :CODMKT is null)
	and ((case
		when HIST.SITUACAO is not null then SANKHYA.OPTION_LABEL('AD_BHZHIST',
		'SITUACAO',
		HIST.SITUACAO)
		when DTENTREGANF is not null then 'Entregue'
		else 'Em Rota'
	end = 'Finalizada'
		and :ENTREGUE = 'S')
	or :ENTREGUE = 'N')
